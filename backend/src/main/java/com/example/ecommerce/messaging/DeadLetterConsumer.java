package com.example.ecommerce.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 死信队列消费者 —— 处理最终失败的消息。
 *
 * 当消息经过多次重试（默认 3 次）仍失败后，会被发送到死信队列（DLQ）。
 * 本组件监听死信队列，记录详细的告警日志，便于人工介入排查问题。
 *
 * 生产环境扩展点：
 *   - 接入钉钉/邮件告警
 *   - 写入告警数据库
 *   - 触发 PagerDuty/Jira 工单
 */
@Component
@ConditionalOnProperty(value = "ecommerce.rabbit.listener-enabled", havingValue = "true")
public class DeadLetterConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterConsumer.class);

    /** 监听订单创建死信队列 */
    @RabbitListener(queues = "${ecommerce.rabbit.order-created-queue}.dlq")
    public void onOrderCreatedDeadLetter(Message message) {
        handleDeadLetter("order-created", message);
    }

    /** 监听支付状态死信队列 */
    @RabbitListener(queues = "${ecommerce.rabbit.payment-status-queue}.dlq")
    public void onPaymentStatusDeadLetter(Message message) {
        handleDeadLetter("payment-status", message);
    }

    /**
     * 统一处理死信消息：记录详细告警日志。
     * 生产环境可扩展为接入钉钉/邮件告警、写入告警数据库表等。
     */
    private void handleDeadLetter(String queueSource, Message message) {
        String messageId = message.getMessageProperties().getMessageId();
        Object retryCount = message.getMessageProperties().getHeaders().get("x-retry-count");
        String lastError = String.valueOf(
            message.getMessageProperties().getHeaders().getOrDefault("x-last-error", "N/A"));

        log.error("""
            ========== 死信队列告警 ==========
            来源队列: {}
            消息ID: {}
            重试次数: {}
            最后一次错误: {}
            ================================
            """, queueSource, messageId, retryCount, lastError);

        // 扩展点：可接入钉钉机器人、邮件通知、告警数据库等
    }
}
