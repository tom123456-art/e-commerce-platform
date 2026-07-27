package com.example.ecommerce.entity;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName payment_callback_log
 */
@Data
public class PaymentCallbackLog implements Serializable {
    /**
     * 
     */
    private Long id;

    /**
     * 
     */
    private String orderNo;

    /**
     * 
     */
    private String tradeNo;

    /**
     * 
     */
    private String tradeStatus;

    /**
     * 
     */
    private String rawPayload;

    /**
     * 
     */
    private Integer verified;

    /**
     * 
     */
    private Integer processed;

    /**
     * 
     */
    private Integer success;

    /**
     * 
     */
    private String errorMessage;

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