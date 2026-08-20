package com.example.ecommerce.mapper;

import com.example.ecommerce.entity.ProductViewEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品浏览事件数据访问接口。
 * <p>
 * 记录用户的每次商品浏览行为，用途：
 * - 用户行为分析：分析浏览偏好与行为模式
 * - 推荐系统：基于浏览历史推荐商品
 * - 热度计算：统计浏览量，用于热门商品排序
 * - 转化率分析：结合购买数据计算浏览->购买的转化率
 */
@Mapper
public interface ProductViewEventMapper {

    /**
     * 插入浏览事件记录（访问商品详情页时调用）。
     * 记录商品 ID、用户 ID（匿名用户为 null）、浏览来源、浏览日期等。
     */
    int insert(ProductViewEvent event);
}