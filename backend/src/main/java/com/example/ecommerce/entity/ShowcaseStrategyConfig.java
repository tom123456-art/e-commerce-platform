package com.example.ecommerce.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 *
 * @TableName showcase_strategy_config
 */
@Data
public class ShowcaseStrategyConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     *
     */
    private Long id;
    /**
     *
     */
    private String mode;
    /**
     *
     */
    private Integer shortWindowDays;
    /**
     *
     */
    private Integer longWindowDays;
    /**
     *
     */
    private BigDecimal cartPreferenceWeight;
    /**
     *
     */
    private String hotWeightsJson;
    /**
     *
     */
    private String anonymousWeightsJson;
    /**
     *
     */
    private String personalizedWeightsJson;
    /**
     *
     */
    private String hotSignalWeightsJson;
    /**
     *
     */
    private Date lastAutoTunedAt;
    /**
     *
     */
    private Date createTime;
    /**
     *
     */
    private Date updateTime;
}