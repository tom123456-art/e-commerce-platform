package com.example.ecommerce.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

/**
 * 商品每日指标实体类。
 *
 * 对应表：product_metric_daily
 * 唯一键：uk_date_product (metric_date, product_id) -- 同一天同一商品只有一条记录
 *
 * 设计模式：事件溯源 -> 聚合
 *   原始浏览事件逐条记录在 product_view_event 表，
 *   再由定时任务聚合（SUM）到本表，供推荐算法和运营报表查询。
 */
@Data
public class ProductMetricDaily implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 统计日期 */
    private LocalDate metricDate;

    /** 商品 ID */
    private Long productId;

    /** 浏览次数 */
    private Integer viewCount;

    /** 加入购物车次数 */
    private Integer cartAddCount;

    /** 付款订单数 */
    private Integer paidOrderCount;

    /** 付款商品数量 */
    private Integer paidQuantity;

    /** 付款总金额 */
    private BigDecimal paidAmount;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}
