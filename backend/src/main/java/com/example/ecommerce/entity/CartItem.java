package com.example.ecommerce.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 *
 * @TableName cart_item
 */
@Data
public class CartItem implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     *
     */
    private Long id;
    /**
     *
     */
    private Long userId;
    /**
     *
     */
    private Long productId;
    /**
     *
     */
    private Integer quantity;
    /**
     *
     */
    private Date createTime;
    /**
     *
     */
    private Date updateTime;
}