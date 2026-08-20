package com.example.ecommerce.messaging;

import com.example.ecommerce.utils.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 订单事件监听器（消费者） —— 处理订单和支付事件。
 *
 * @Component：标记为 Spring 组件
 * @ConditionalOnProperty：只在 ecommerce.rabbit.listener-enabled=true 时启用
 * 测试环境可设为 false 禁用消费者
 * @RabbitListener：声明式消费者注解，Spring AMQP 会自动：
 * 1. 连接 RabbitMQ
 * 2. 监听指定队列
 * 3. 收到消息时调用对应方法
 * 4. 自动 ACK（方法正常返回）或 NACK（方法抛异常）
 */
@Component
@ConditionalOnProperty(value = "ecommerce.rabbit.listener-enabled", havingValue = "true")
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final RedisUtil redisUtil;
    private final OrderMessagePublisher orderMessagePublisher;
    private final String orderCreatedQueue;
    private final String paymentStatusQueue;
    /**
     * 最大重试次数，超过后进入死信队列
     */
    private final int maxRetries;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderEventListener(RedisUtil redisUtil,
                              OrderMessagePublisher orderMessagePublisher,
                              @Value("${ecommerce.rabbit.order-created-queue}") String orderCreatedQueue,
                              @Value("${ecommerce.rabbit.payment-status-queue}") String paymentStatusQueue,
                              @Value("${ecommerce.rabbit.max-retries:3}") int maxRetries) {
        this.redisUtil = redisUtil;
        this.orderMessagePublisher = orderMessagePublisher;
        this.orderCreatedQueue = orderCreatedQueue;
        this.paymentStatusQueue = paymentStatusQueue;
        this.maxRetries = maxRetries;
    }

    /**
     * 消费"订单已创建"消息。
     *
     * @RabbitListener(queues = "${ecommerce.rabbit.order-created-queue}")：
     * 监听配置项指定的队列
     * <p>
     * 处理逻辑：
     * 1. 将消息快照存入 Redis（用于幂等性检查和数据追踪）
     * 2. 记录消费日志
     * <p>
     * 失败处理：
     * - 异常时调用 process 方法，根据重试次数决定：
     * a. 未超最大重试次数：抛出 AmqpRejectAndDontRequeueException，
     * 消息进入重试队列，TTL 到期后重新投递
     * b. 超过最大重试次数：发送到死信队列，人工处理
     */
    @RabbitListener(queues = "${ecommerce.rabbit.order-created-queue}")
    public void onOrderCreated(OrderCreatedMessage payload, Message rawMessage) {
        process(payload.getOrderNo(), orderCreatedQueue, rawMessage, payload, () -> {
            // 业务逻辑：将消息快照存入 Redis（TTL 24 小时）
            store("mq:last:order:" + payload.getOrderNo(), payload);
            log.info("Consumed order created message for {}", payload.getOrderNo());
        });
    }

    /**
     * 消费"支付状态变更"消息。
     */
    @RabbitListener(queues = "${ecommerce.rabbit.payment-status-queue}")
    public void onPaymentStatus(PaymentStatusMessage payload, Message rawMessage) {
        process(payload.getOrderNo(), paymentStatusQueue, rawMessage, payload, () -> {
            store("mq:last:payment:" + payload.getOrderNo(), payload);
            log.info("Consumed payment status message for {} => {}",
                    payload.getOrderNo(), payload.getTradeStatus());
        });
    }

    /**
     * 将消息快照存入 Redis（用于幂等性检查和数据追踪）
     */
    private void store(String key, Object value) {
        try {
            redisUtil.set(key, objectMapper.writeValueAsString(value), 86400);  // TTL 24 小时
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to store MQ snapshot", ex);
        }
    }

    /**
     * 统一的消息处理框架（模板方法模式）。
     * <p>
     * 流程：
     * 1. 执行业务逻辑（action.run()）
     * 2. 失败时检查重试次数：
     * a. 已超最大重试次数 → 发送到死信队列
     * b. 未超 → 抛出 AmqpRejectAndDontRequeueException，
     * 消息被拒绝且不重新入队，自动进入重试队列（由 x-dead-letter-exchange 配置）
     *
     * @param businessKey 业务键（如订单号），用于日志标识
     * @param queueName   队列名，用于从 x-death header 提取重试次数
     * @param rawMessage  原始消息（含 headers）
     * @param payload     反序列化的消息体
     * @param action      业务逻辑（Runnable）
     */
    private void process(String businessKey, String queueName, Message rawMessage,
                         Object payload, Runnable action) {
        try {
            action.run();
        } catch (Exception ex) {
            // 从 x-death header 提取重试次数（RabbitMQ 死信机制自动维护）
            int retryCount = extractRetryCount(rawMessage, queueName);
            if (retryCount >= maxRetries) {
                // 超过最大重试次数，发送到死信队列
                log.error("Message {} failed after {} retries, sending to dead-letter queue",
                        businessKey, retryCount, ex);
                orderMessagePublisher.publishToDeadLetter(queueName, payload, retryCount, ex.getMessage());
                return;
            }
            // 未超最大重试次数，拒绝消息（不重新入队），消息进入重试队列
            log.warn("Message {} failed on retry {}, routing to retry queue",
                    businessKey, retryCount + 1, ex);
            throw new AmqpRejectAndDontRequeueException(
                    "Message processing failed, routed to retry queue", ex);
        }
    }

    /**
     * 从消息的 x-death header 提取重试次数。
     * <p>
     * RabbitMQ 死信机制会自动在消息头维护 x-death 信息，
     * 记录消息被死信的次数、来源队列等。
     *
     * @param rawMessage 原始消息
     * @param queueName  队列名（用于匹配 x-death 中的来源队列）
     * @return 重试次数（0 表示第一次失败）
     */
    @SuppressWarnings("unchecked")
    private int extractRetryCount(Message rawMessage, String queueName) {
        if (rawMessage == null) {
            return 0;
        }
        Object xDeathHeader = rawMessage.getMessageProperties().getHeaders().get("x-death");
        if (!(xDeathHeader instanceof List)) {
            return 0;
        }
        for (Object item : (List<Object>) xDeathHeader) {
            if (!(item instanceof Map<?, ?> death)) {
                continue;
            }
            if (!queueName.equals(String.valueOf(death.get("queue")))) {
                continue;
            }
            Object count = death.get("count");
            if (count instanceof Number) {
                return ((Number) count).intValue();
            }
        }
        return 0;
    }
}
