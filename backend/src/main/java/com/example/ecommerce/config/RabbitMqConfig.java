package com.example.ecommerce.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 消息队列配置类 -- 定义"重试 + 死信"模式的完整消息架构。
 *
 * 架构概览（以订单创建队列为例）：
 *
 *   [生产者] -> [Event Exchange] -> [order-created 主队列] -> [消费者]
 *                                       ↓ (消费失败，拒绝)
 *                              [Retry Exchange] -> [order-created.retry 重试队列 (TTL=30s)]
 *                                       ↓ (TTL 到期，死信回主交换机)
 *                              [Event Exchange] -> [主队列] (重新消费)
 *                                       ↓ (超过最大重试次数)
 *                              [DLQ Exchange] -> [order-created.dlq 死信队列] -> [人工处理]
 *
 * 核心机制：
 *   - 主队列配置 x-dead-letter-exchange -> 消费失败的消息路由到重试交换机
 *   - 重试队列配置 x-message-ttl -> 消息存活 30 秒后死信回主交换机（实现延迟重试）
 *   - 死信队列存储最终失败的消息，等待人工介入
 *
 * @ConditionalOnProperty：只在 ecommerce.rabbit.enabled=true 时加载
 *   测试环境设为 false 可跳过 RabbitMQ 配置
 */
@Configuration
@ConditionalOnProperty(prefix = "ecommerce.rabbit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMqConfig {

    // 从 application.yml 读取队列/交换机名称
    @Value("${ecommerce.rabbit.exchange}")
    private String eventExchange;

    @Value("${ecommerce.rabbit.retry-exchange}")
    private String retryExchange;

    @Value("${ecommerce.rabbit.dead-letter-exchange}")
    private String deadLetterExchange;

    @Value("${ecommerce.rabbit.order-created-queue}")
    private String orderCreatedQueue;

    @Value("${ecommerce.rabbit.payment-status-queue}")
    private String paymentStatusQueue;

    /** 重试队列消息存活时间（毫秒），TTL 到期后死信回主交换机 */
    private static final int RETRY_TTL_MS = 30000;

    // ==================== 交换机（Exchange）====================

    /** 主事件交换机：生产者发送消息的入口 */
    @Bean
    public DirectExchange eventDirectExchange() {
        return new DirectExchange(eventExchange, true, false);
    }

    /** 重试交换机：接收消费失败的消息，路由到重试队列 */
    @Bean
    public DirectExchange retryDirectExchange() {
        return new DirectExchange(retryExchange, true, false);
    }

    /** 死信交换机：接收超过最大重试次数的消息 */
    @Bean
    public DirectExchange deadLetterDirectExchange() {
        return new DirectExchange(deadLetterExchange, true, false);
    }

    // ==================== 队列（Queue）====================

    /**
     * 订单创建主队列。
     * 配置死信路由：消费失败时消息发送到 retryExchange + 路由键 orderCreatedQueue.retry
     */
    @Bean
    public Queue orderCreatedQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", retryExchange);
        args.put("x-dead-letter-routing-key", orderCreatedQueue + ".retry");
        return new Queue(orderCreatedQueue, true, false, false, args);
    }

    /**
     * 订单创建重试队列。
     * 配置 TTL：消息存活 30 秒后死信回主交换机（eventExchange），实现延迟重试。
     */
    @Bean
    public Queue orderCreatedRetryQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", RETRY_TTL_MS);
        args.put("x-dead-letter-exchange", eventExchange);
        args.put("x-dead-letter-routing-key", orderCreatedQueue);
        return new Queue(orderCreatedQueue + ".retry", true, false, false, args);
    }

    /** 订单创建死信队列：存储最终失败的消息 */
    @Bean
    public Queue orderCreatedDeadLetterQueue() {
        return new Queue(orderCreatedQueue + ".dlq", true);
    }

    /** 支付状态主队列（结构同订单创建队列） */
    @Bean
    public Queue paymentStatusQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", retryExchange);
        args.put("x-dead-letter-routing-key", paymentStatusQueue + ".retry");
        return new Queue(paymentStatusQueue, true, false, false, args);
    }

    /** 支付状态重试队列 */
    @Bean
    public Queue paymentStatusRetryQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", RETRY_TTL_MS);
        args.put("x-dead-letter-exchange", eventExchange);
        args.put("x-dead-letter-routing-key", paymentStatusQueue);
        return new Queue(paymentStatusQueue + ".retry", true, false, false, args);
    }

    /** 支付状态死信队列 */
    @Bean
    public Queue paymentStatusDeadLetterQueue() {
        return new Queue(paymentStatusQueue + ".dlq", true);
    }

    // ==================== 绑定（Binding）====================

    /** 主队列绑定到主交换机（路由键 = 队列名） */
    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder.bind(orderCreatedQueue()).to(eventDirectExchange()).with(orderCreatedQueue);
    }

    /** 重试队列绑定到重试交换机 */
    @Bean
    public Binding orderCreatedRetryBinding() {
        return BindingBuilder.bind(orderCreatedRetryQueue()).to(retryDirectExchange()).with(orderCreatedQueue + ".retry");
    }

    /** 死信队列绑定到死信交换机 */
    @Bean
    public Binding orderCreatedDlqBinding() {
        return BindingBuilder.bind(orderCreatedDeadLetterQueue()).to(deadLetterDirectExchange()).with(orderCreatedQueue + ".dlq");
    }

    @Bean
    public Binding paymentStatusBinding() {
        return BindingBuilder.bind(paymentStatusQueue()).to(eventDirectExchange()).with(paymentStatusQueue);
    }

    @Bean
    public Binding paymentStatusRetryBinding() {
        return BindingBuilder.bind(paymentStatusRetryQueue()).to(retryDirectExchange()).with(paymentStatusQueue + ".retry");
    }

    @Bean
    public Binding paymentStatusDlqBinding() {
        return BindingBuilder.bind(paymentStatusDeadLetterQueue()).to(deadLetterDirectExchange()).with(paymentStatusQueue + ".dlq");
    }

    // ==================== 基础设施 Bean ====================

    /** 消息转换器：使用 Jackson 将 Java 对象序列化为 JSON 消息体 */
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate：消息发送模板。
     * 配置 Publisher Confirm 回调，确认消息是否到达 Broker。
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        template.setMandatory(true);  // 消息无法路由时触发 returns 回调
        // Publisher Confirm：消息到达 Broker 时回调
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack && correlationData != null) {
                // 生产环境应接入告警系统
            }
        });
        return template;
    }
}
