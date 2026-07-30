package com.example.ecommerce.mapper;

import com.example.ecommerce.entity.Store;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商家店铺数据访问接口。
 * 店铺与商家一对一，通过 merchant_id 关联。
 */
@Mapper
public interface StoreMapper {

    /** 根据商家 ID 查询店铺（商家中心展示店铺信息） */
    Store selectByMerchantId(Long merchantId);

    /** 插入店铺（商家首次创建店铺） */
    void insert(Store store);

    /** 更新店铺信息（按主键 id 定位） */
    void update(Store store);
}