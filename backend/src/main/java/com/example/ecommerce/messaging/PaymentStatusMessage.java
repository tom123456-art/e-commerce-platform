package com.example.ecommerce.messaging;

import java.io.Serializable;

/**
 * 支付状态变更事件消息 DTO —— 用于 RabbitMQ 异步消息传递。
 * <p>
 * 只包含必要的支付状态信息，用于消费者异步处理。
 * <p>
 * 消费者用途：
 * - 异步更新订单状态
 * - 发送支付结果通知
 * - 记录支付统计
 */
public class PaymentStatusMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单号（业务唯一标识）
     */
    private String orderNo;

    /**
     * 交易状态（如：TRADE_SUCCESS、TRADE_FAILED、WAIT_BUYER_PAY）
     */
    private String tradeStatus;

    /**
     * 支付是否成功
     */
    private boolean success;

    // ==================== Getter / Setter ====================

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getTradeStatus() {
        return tradeStatus;
    }

    public void setTradeStatus(String tradeStatus) {
        this.tradeStatus = tradeStatus;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    @Override
    public String toString() {
        return "PaymentStatusMessage{" +
                "orderNo='" + orderNo + '\'' +
                ", tradeStatus='" + tradeStatus + '\'' +
                ", success=" + success +
                '}';
    }
}
