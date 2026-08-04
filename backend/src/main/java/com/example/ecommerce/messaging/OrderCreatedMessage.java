package com.example.ecommerce.messaging;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 订单创建事件消息 DTO —— 用于 RabbitMQ 异步消息传递。
 *
 * 只包含必要的业务信息，不包含订单的完整数据（减少消息体积）。
 * 实现 Serializable 接口，确保消息可以被正确序列化和反序列化。
 *
 * 消费者用途：
 *   - 异步扣减库存
 *   - 发送通知（短信、站内信）
 *   - 更新统计指标
 */
public class OrderCreatedMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单号（业务唯一标识） */
    private String orderNo;

    /** 用户 ID */
    private Long userId;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 订单中的商品 ID 列表 */
    private List<Long> productIds;

    // ==================== Getter / Setter ====================

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<Long> getProductIds() {
        return productIds;
    }

    public void setProductIds(List<Long> productIds) {
        this.productIds = productIds;
    }

    @Override
    public String toString() {
        return "OrderCreatedMessage{" +
            "orderNo='" + orderNo + '\'' +
            ", userId=" + userId +
            ", totalAmount=" + totalAmount +
            ", productIds=" + productIds +
            '}';
    }
}
