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

    private static final long serialVersionUID = 1L;
}