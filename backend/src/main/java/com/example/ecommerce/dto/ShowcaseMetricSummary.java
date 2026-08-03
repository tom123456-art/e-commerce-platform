package com.example.ecommerce.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 展示策略指标汇总 DTO。
 *
 * 由 ProductMetricDailyMapper.aggregateSummary() 查询返回：
 *   SELECT IFNULL(SUM(view_count), 0) AS viewCount, ...
 *
 * 列别名（as viewCount）与本类属性名一致，MyBatis 自动映射，无需 resultMap。
 */
@Data
public class ShowcaseMetricSummary implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 总浏览次数 */
    private Integer viewCount;

    /** 总加购次数 */
    private Integer cartAddCount;

    /** 总付款订单数 */
    private Integer paidOrderCount;

    /** 总付款商品数量 */
    private Integer paidQuantity;

    /** 总付款金额 */
    private BigDecimal paidAmount;
}
