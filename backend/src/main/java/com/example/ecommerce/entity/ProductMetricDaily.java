package com.example.ecommerce.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

/**
 *
 * @TableName product_metric_daily
 */
@Data
public class ProductMetricDaily implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     *
     */
    private Long id;
    /**
     *
     */
    private LocalDate metricDate;
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
}