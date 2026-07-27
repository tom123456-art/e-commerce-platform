package com.example.ecommerce.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName product_metric_daily
 */
@Data
public class ProductMetricDaily implements Serializable {
    /**
     * 
     */
    private Long id;

    /**
     * 
     */
    private Date metricDate;

    /**
     * 
     */
    private Long productId;

    /**
     * 
     */
    private Integer viewCount;

    /**
     * 
     */
    private Integer cartAddCount;

    /**
     * 
     */
    private Integer paidOrderCount;

    /**
     * 
     */
    private Integer paidQuantity;

    /**
     * 
     */
    private BigDecimal paidAmount;

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