package com.example.ecommerce.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 展示策略每日指标 DTO。
 *
 * 由 ProductMetricDailyMapper.selectRecentDailyTotals() 查询返回：
 *   SELECT metric_date AS metricDate, IFNULL(SUM(view_count), 0) AS viewCount, ...
 *   GROUP BY metric_date ORDER BY metric_date DESC LIMIT #{limit}
 *
 * 用于首页趋势图：展示最近 N 天每天的浏览/加购/付款指标变化。
 */
@Data
public class ShowcaseDailyMetricItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 统计日期 */
    private LocalDate metricDate;

    /** 当日浏览次数 */
    private Integer viewCount;

    /** 当日加购次数 */
    private Integer cartAddCount;

    /** 当日付款订单数 */
    private Integer paidOrderCount;

    /** 当日付款商品数量 */
    private Integer paidQuantity;

    /** 当日付款金额 */
    private BigDecimal paidAmount;
}
