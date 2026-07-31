package com.example.ecommerce.mapper;

import com.example.ecommerce.entity.ProductMetricDaily;
import org.apache.ibatis.annotations.Mapper;

/**
* @author 仝佳昀
* @description 针对表【product_metric_daily】的数据库操作Mapper
* @createDate 2026-07-30 15:53:29
* @Entity com.example.ecommerce.entity.ProductMetricDaily
*/
@Mapper
public interface ProductMetricDailyMapper {
    //增量更新每日指标 UPSERT模式
    int upsertDelete(ProductMetricDaily metricDaily);

}




