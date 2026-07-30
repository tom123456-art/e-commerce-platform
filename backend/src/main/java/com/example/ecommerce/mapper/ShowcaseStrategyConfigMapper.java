package com.example.ecommerce.mapper;

import com.example.ecommerce.entity.ShowcaseStrategyConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品展示策略配置数据访问接口。
 *
 * 采用"单行配置表"模式：表中只有一条记录，存储所有展示算法参数
 * （mode、窗口期、各维度权重、购物车偏好权重等）。
 */
@Mapper
public interface ShowcaseStrategyConfigMapper {

    /**
     * 查询当前展示策略配置（单行配置表，返回唯一记录）。
     * ORDER BY id ASC LIMIT 1 确保即使异常出现多条也稳定返回最早创建的那条。
     */
    ShowcaseStrategyConfig selectCurrent();

    /**
     * 插入或更新配置（UPSERT 模式）。
     * 表为空时 INSERT 初始化，记录已存在时 UPDATE 更新，一条 SQL 完成。
     */
    int upsert(ShowcaseStrategyConfig config);
}