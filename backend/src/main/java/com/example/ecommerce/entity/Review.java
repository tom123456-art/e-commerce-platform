package com.example.ecommerce.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 *
 * @TableName review
 */
@Data
public class Review implements Serializable {
    private static final long serialVersionUID = 1L;
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
}