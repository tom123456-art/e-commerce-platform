package com.example.ecommerce.entity;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName review
 */
@Data
public class Review implements Serializable {
    /**
     * 
     */
    private Long id;

    /**
     * 
     */
    private Long productId;

    /**
     * 
     */
    private Long userId;

    /**
     * 
     */
    private Long orderId;

    /**
     * 
     */
    private Integer rating;

    /**
     * 
     */
    private String content;

    /**
     * 
     */
    private String reply;

    /**
     * 
     */
    private Date replyTime;

    /**
     * 
     */
    private Integer status;

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