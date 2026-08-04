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
 * RabbitMQ 配置类
 * 实现了基于死信队列的消息重试机制
 */
@Configuration
@ConditionalOnProperty(prefix = "ecommerce.rabbit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMqConfig {

    // ==================== 属性注入 (已修复损坏的注解) ====================

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

    // 重试延迟时间：30秒
    private static final int RETRY_TTL_MS = 30000;

    // ==================== 交换机定义 ====================

    /**
     * 业务主交换机
     */
    @Bean
    public DirectExchange eventDirectExchange() {
        return new DirectExchange(eventExchange, true, false);
    }

    /**
     * 重试消息中转交换机
     */
    @Bean
    public DirectExchange retryDirectExchange() {
        return new DirectExchange(retryExchange, true, false);
    }

    /**
     * 最终死信交换机（存储彻底失败的消息）
     */
    @Bean
    public DirectExchange deadLetterDirectExchange() {
        return new DirectExchange(deadLetterExchange, true, false);
    }

    // ==================== 订单创建相关队列 ====================

    /**
     * 订单创建主队列
     * 配置：消息处理失败 -> 发送到 retryExchange
     */
    @Bean
    public Queue orderCreatedQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", retryExchange);
        args.put("x-dead-letter-routing-key", orderCreatedQueue + ".retry");
        return new Queue(orderCreatedQueue, true, false, false, args);
    }

    /**
     * 订单创建重试队列（延迟队列）
     * 配置：TTL 30秒 -> 过期后发送回 eventExchange
     */
    @Bean
    public Queue orderCreatedRetryQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", RETRY_TTL_MS);
        args.put("x-dead-letter-exchange", eventExchange);
        args.put("x-dead-letter-routing-key", orderCreatedQueue);
        return new Queue(orderCreatedQueue + ".retry", true, false, false, args);
    }

    /**
     * 订单创建死信队列（最终失败）
     */
    @Bean
    public Queue orderCreatedDeadLetterQueue() {
        return new Queue(orderCreatedQueue + ".dlq", true);
    }

    // ==================== 支付状态相关队列 ====================

    /**
     * 支付状态主队列
     */
    @Bean
    public Queue paymentStatusQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", retryExchange);
        args.put("x-dead-letter-routing-key", paymentStatusQueue + ".retry");
        return new Queue(paymentStatusQueue, true, false, false, args);
    }

    /**
     * 支付状态重试队列
     */
    @Bean
    public Queue paymentStatusRetryQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", RETRY_TTL_MS);
        args.put("x-dead-letter-exchange", eventExchange);
        args.put("x-dead-letter-routing-key", paymentStatusQueue);
        return new Queue(paymentStatusQueue + ".retry", true, false, false, args);
    }

    /**
     * 支付状态死信队列
     */
    @Bean
    public Queue paymentStatusDeadLetterQueue() {
        return new Queue(paymentStatusQueue + ".dlq", true);
    }

    // ==================== 绑定关系 ====================

    // --- 订单创建绑定 ---
    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder.bind(orderCreatedQueue()).to(eventDirectExchange()).with(orderCreatedQueue);
    }

    @Bean
    public Binding orderCreatedRetryBinding() {
        return BindingBuilder.bind(orderCreatedRetryQueue()).to(retryDirectExchange()).with(orderCreatedQueue + ".retry");
    }

    @Bean
    public Binding orderCreatedDeadLetterBinding() {
        return BindingBuilder.bind(orderCreatedDeadLetterQueue()).to(deadLetterDirectExchange()).with(orderCreatedQueue + ".dlq");
    }

    // --- 支付状态绑定 ---
    @Bean
    public Binding paymentStatusBinding() {
        return BindingBuilder.bind(paymentStatusQueue()).to(eventDirectExchange()).with(paymentStatusQueue);
    }

    @Bean
    public Binding paymentStatusRetryBinding() {
        return BindingBuilder.bind(paymentStatusRetryQueue()).to(retryDirectExchange()).with(paymentStatusQueue + ".retry");
    }

    @Bean
    public Binding paymentStatusDeadLetterBinding() {
        return BindingBuilder.bind(paymentStatusDeadLetterQueue()).to(deadLetterDirectExchange()).with(paymentStatusQueue + ".dlq");
    }

    // ==================== 模板配置 ====================

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        // 设置 JSON 序列化转换器
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        return template;
    }
}