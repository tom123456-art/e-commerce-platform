package com.example.ecommerce.messaging;

import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 订单消息生产者 —— 发布订单和支付事件到 RabbitMQ。
 *
 * @Service：标记为 Spring Service 组件
 *
 * 职责：
 *   - publishOrderCreated：订单创建后发布消息（异步扣减库存、发送通知等）
 *   - publishPaymentStatus：支付状态变更后发布消息（异步更新订单状态）
 *   - publishToDeadLetter：超过最大重试次数后，将消息发送到死信队列
 *
 * 设计要点：
 *   1. 优雅降级：RabbitMQ 不可用时（rabbitEnabled=false），消息发布跳过，不影响主业务
 *   2. Publisher Confirm：消息到达 Broker 时回调确认
 *   3. 消息元数据：携带 messageId、发送时间、路由键等，便于追踪
 */
@Service
public class OrderMessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderMessagePublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final boolean rabbitEnabled;
    private final String exchange;
    private final String deadLetterExchange;
    private final String orderCreatedQueue;
    private final String paymentStatusQueue;

    /**
     * 构造器注入，通过 @Value 读取 RabbitMQ 配置。
     *
     * @Value("${ecommerce.rabbit.enabled:true}")：
     *   读取 ecommerce.rabbit.enabled 配置项，默认 true（配置不存在时启用）
     *   测试环境可设为 false 禁用 RabbitMQ
     */
    public OrderMessagePublisher(RabbitTemplate rabbitTemplate,
                                 @Value("${ecommerce.rabbit.enabled:true}") boolean rabbitEnabled,
                                 @Value("${ecommerce.rabbit.exchange}") String exchange,
                                 @Value("${ecommerce.rabbit.dead-letter-exchange}") String deadLetterExchange,
                                 @Value("${ecommerce.rabbit.order-created-queue}") String orderCreatedQueue,
                                 @Value("${ecommerce.rabbit.payment-status-queue}") String paymentStatusQueue) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitEnabled = rabbitEnabled;
        this.exchange = exchange;
        this.deadLetterExchange = deadLetterExchange;
        this.orderCreatedQueue = orderCreatedQueue;
        this.paymentStatusQueue = paymentStatusQueue;
    }

    /**
     * 发布"订单已创建"消息。
     *
     * 触发场景：用户下单成功后，OrderService 调用此方法。
     * 消费者收到消息后可以：扣减库存、发送通知、更新统计等（异步处理）。
     *
     * 优雅降级：rabbitEnabled=false 或 order=null 时直接返回，不发布消息
     */
    public void publishOrderCreated(Order order, List<OrderItem> orderItems) {
        if (!rabbitEnabled || order == null) {
            return;  // RabbitMQ 禁用或订单为空，跳过
        }
        // 构造消息体（DTO，包含订单关键信息）
        OrderCreatedMessage message = new OrderCreatedMessage();
        message.setOrderNo(order.getOrderNo());
        message.setUserId(order.getUserId());
        message.setTotalAmount(order.getTotalAmount());
        message.setProductIds(orderItems == null
            ? Collections.emptyList()
            : orderItems.stream().map(OrderItem::getProductId).collect(Collectors.toList()));
        // 发送到主交换机，路由键为队列名（约定：路由键 = 队列名）
        send(exchange, orderCreatedQueue, message, Collections.emptyMap(), true);
    }

    /**
     * 发布"支付状态变更"消息。
     * 触发场景：支付回调验签成功后，PaymentService 调用此方法。
     */
    public void publishPaymentStatus(String orderNo, String tradeStatus, boolean success) {
        if (!rabbitEnabled || orderNo == null || orderNo.trim().isEmpty()) {
            return;
        }
        PaymentStatusMessage message = new PaymentStatusMessage();
        message.setOrderNo(orderNo);
        message.setTradeStatus(tradeStatus);
        message.setSuccess(success);
        send(exchange, paymentStatusQueue, message, Collections.emptyMap(), true);
    }

    /**
     * 将消息发送到死信队列（超过最大重试次数后调用）。
     *
     * 携带诊断信息：
     *   - x-original-routing-key：原始路由键
     *   - x-retry-count：重试次数
     *   - x-dead-letter-at：进入死信队列的时间
     *   - x-last-error：最后一次错误信息
     */
    public void publishToDeadLetter(String originalRoutingKey, Object payload, int retryCount, String errorMessage) {
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put("x-original-routing-key", originalRoutingKey);
        headers.put("x-retry-count", retryCount);
        headers.put("x-dead-letter-at", Instant.now().toString());
        if (errorMessage != null && !errorMessage.trim().isEmpty()) {
            headers.put("x-last-error", errorMessage);
        }
        // 发送到死信交换机，swallowErrors=false（失败时抛异常）
        send(deadLetterExchange, originalRoutingKey + ".dlq", payload, headers, false);
    }

    /**
     * 底层发送方法。
     *
     * CorrelationData：Publisher Confirm 机制的回调 ID，用于确认消息是否到达 Broker
     * enrichMessage：在发送前为消息添加元数据（messageId、发送时间、路由键）
     *
     * @param swallowErrors true=吞掉异常（主业务消息），false=抛出异常（死信消息）
     */
    private void send(String exchangeName, String routingKey, Object payload,
                      Map<String, Object> headers, boolean swallowErrors) {
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, payload,
                message -> enrichMessage(message, routingKey, headers, correlationData),
                correlationData);
        } catch (Exception ex) {
            if (swallowErrors) {
                // 主业务消息发送失败不影响业务（消息丢失比阻塞业务更可接受）
                log.warn("RabbitMQ publish skipped: {}", ex.getMessage());
                return;
            }
            throw ex;  // 死信消息发送失败必须抛出，否则消息会彻底丢失
        }
    }

    /** 为消息添加元数据：messageId、路由键、发送时间、自定义 headers */
    private Message enrichMessage(Message message, String routingKey,
                                  Map<String, Object> headers, CorrelationData correlationData) {
        message.getMessageProperties().setMessageId(correlationData.getId());
        message.getMessageProperties().setHeader("x-routing-key", routingKey);
        message.getMessageProperties().setHeader("x-sent-at", Instant.now().toString());
        if (headers != null) {
            headers.forEach((key, value) -> message.getMessageProperties().setHeader(key, value));
        }
        return message;
    }
}
