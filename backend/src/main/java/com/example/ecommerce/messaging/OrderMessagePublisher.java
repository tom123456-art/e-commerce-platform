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
 * 订单/支付消息发布器。
 *
 * 【设计要点】
 *   1. 通过 @Value 注入队列/交换机名称，与 RabbitMqConfig 中声明保持一致
 *   2. rabbitEnabled=false 时所有 publish 方法静默返回（开发环境无 RabbitMQ 时降级）
 *   3. swallowErrors=true：发布失败仅记日志不抛异常，避免影响主流程（订单已创建，MQ 失败不应回滚）
 *   4. 死信队列：消费失败超过 maxRetries 次后转入死信队列，保留原始消息+错误信息
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

    /** 发布"订单创建"事件 */
    public void publishOrderCreated(Order order, List<OrderItem> orderItems) {
        if (!rabbitEnabled || order == null) return;
        OrderCreatedMessage msg = new OrderCreatedMessage();
        msg.setOrderNo(order.getOrderNo());
        msg.setUserId(order.getUserId());
        msg.setTotalAmount(order.getTotalAmount());
        msg.setProductIds(orderItems == null ? Collections.emptyList()
            : orderItems.stream().map(OrderItem::getProductId).collect(Collectors.toList()));
        send(exchange, orderCreatedQueue, msg, Collections.emptyMap(), true);
    }

    /** 发布"支付状态"事件 */
    public void publishPaymentStatus(String orderNo, String tradeStatus, boolean success) {
        if (!rabbitEnabled || orderNo == null || orderNo.trim().isEmpty()) return;
        PaymentStatusMessage msg = new PaymentStatusMessage();
        msg.setOrderNo(orderNo);
        msg.setTradeStatus(tradeStatus);
        msg.setSuccess(success);
        send(exchange, paymentStatusQueue, msg, Collections.emptyMap(), true);
    }

    /** 转发到死信队列（附带原始 routing key、重试次数、错误信息） */
    public void publishToDeadLetter(String originalRoutingKey, Object payload, int retryCount, String errorMessage) {
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put("x-original-routing-key", originalRoutingKey);
        headers.put("x-retry-count", retryCount);
        headers.put("x-dead-letter-at", Instant.now().toString());
        if (errorMessage != null && !errorMessage.trim().isEmpty()) {
            headers.put("x-last-error", errorMessage);
        }
        send(deadLetterExchange, deadLetterRoutingKey(originalRoutingKey), payload, headers, false);
    }

    /** 实际发送方法（封装 CorrelationData + 头信息） */
    private void send(String exchangeName, String routingKey, Object payload,
                      Map<String, Object> headers, boolean swallowErrors) {
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, payload,
                message -> enrichMessage(message, routingKey, headers, correlationData), correlationData);
        } catch (Exception ex) {
            if (swallowErrors) {
                log.warn("RabbitMQ publish skipped: {}", ex.getMessage());
                return;
            }
            throw ex;
        }
    }

    /** 给消息添加 messageId、routing key、发送时间戳等头信息 */
    private Message enrichMessage(Message message, String routingKey,
                                  Map<String, Object> headers, CorrelationData correlationData) {
        message.getMessageProperties().setMessageId(correlationData.getId());
        message.getMessageProperties().setHeader("x-routing-key", routingKey);
        message.getMessageProperties().setHeader("x-sent-at", Instant.now().toString());
        if (headers != null) {
            headers.forEach((k, v) -> message.getMessageProperties().setHeader(k, v));
        }
        return message;
    }

    private String deadLetterRoutingKey(String routingKey) {
        return routingKey + ".dlq";
    }
}
