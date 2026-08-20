package com.example.ecommerce.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单明细
 *
 * @TableName order_item
 */
@Data
public class OrderItem implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     *
     */
    private Long id;
    /**
     *
     */
    private Long orderId;
    /**
     *
     */
    private Long productId;
    /**
     *
     */
    private String productName;
    /**
     *
     */
    private BigDecimal price;
    /**
     *
     */
    private Integer quantity;
}