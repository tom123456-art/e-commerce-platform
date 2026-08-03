package com.example.ecommerce.mapper;

import com.example.ecommerce.dto.ShowcaseDailyMetricItem;
import com.example.ecommerce.dto.ShowcaseMetricSummary;
import com.example.ecommerce.entity.ProductMetricDaily;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 商品每日指标数据访问接口。
 *
 * 特点：
 *   - UPSERT：upsertDelta 用 ON DUPLICATE KEY UPDATE 实现"有则累加，无则插入"
 *   - 聚合查询：aggregateSummary 用 SUM 汇总指标
 *   - DTO 直接映射：查询结果用列别名直接映射到 DTO，无需 resultMap
 */
@Mapper
public interface ProductMetricDailyMapper {

    /**
     * 增量更新每日指标（UPSERT 模式）。
     * 若 metric_date+product_id 不存在则插入，存在则累加：
     *   ON DUPLICATE KEY UPDATE view_count = view_count + VALUES(view_count), ...
     */
    int upsertDelta(ProductMetricDaily metric);

    /** 汇总指定日期之后的所有指标（SUM + IFNULL，返回 DTO） */
    ShowcaseMetricSummary aggregateSummary(@Param("dateFrom") LocalDate dateFrom);

    /** 查询最近 N 天的每日指标汇总（GROUP BY 日期，用于趋势图） */
    List<ShowcaseDailyMetricItem> selectRecentDailyTotals(@Param("limit") Integer limit);
}