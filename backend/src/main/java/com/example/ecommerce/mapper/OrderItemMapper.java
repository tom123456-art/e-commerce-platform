package com.example.ecommerce.mapper;

import com.example.ecommerce.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
* @author waqwb
* @description 针对表【order_item】的数据库操作Mapper
* @createDate 2026-07-29 15:54:56
* @Entity com.example.ecommerce.entity.OrderItem
*/
@Mapper
public interface OrderItemMapper {
    // 查询所有订单项
    List<OrderItem> selectAll();

    // 根据订单ID查询订单项
    List<OrderItem> selectByOrderId(Long orderId);

    // 插入单个订单项
    int insert(OrderItem orderItem);

    // 批量插入订单项
    int insertBatch(List<OrderItem> orderItems);
}




