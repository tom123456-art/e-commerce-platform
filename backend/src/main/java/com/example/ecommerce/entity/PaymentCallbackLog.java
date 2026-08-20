package com.example.ecommerce.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 *
 * @TableName payment_callback_log
 */
@Data
public class PaymentCallbackLog implements Serializable {
    private static final long serialVersionUID = 1L;
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
    private Boolean verified;
    /**
     *
     */
    private Boolean processed;
    /**
     *
     */
    private Boolean success;
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
}