package com.example.ecommerce.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName order
 */
@Data
public class Order implements Serializable {
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
    private String orderNo;

    /**
     * 
     */
    private BigDecimal totalAmount;

    /**
     * 
     */
    private Integer status;

    /**
     * 
     */
    private String address;

    /**
     * 
     */
    private String phone;

    /**
     * 
     */
    private String receiver;

    /**
     * 
     */
    private Date createTime;

    /**
     * 
     */
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}