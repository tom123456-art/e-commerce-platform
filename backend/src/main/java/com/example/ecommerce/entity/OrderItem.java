package com.example.ecommerce.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 
 * @TableName order_item
 */
@Data
public class OrderItem implements Serializable {
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

    private static final long serialVersionUID = 1L;
}