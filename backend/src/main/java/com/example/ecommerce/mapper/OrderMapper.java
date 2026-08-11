package com.example.ecommerce.mapper;

import com.example.ecommerce.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author waqwb
 * @description 针对表【order】的数据库操作Mapper
 * @createDate 2026-07-29 14:09:38
 * @Entity com.example.ecommerce.entity.Order
 */
@Mapper
public interface OrderMapper {
    // 根据id查询订单
    Order selectById(Long id);

    // 根据业务订单号查询
    Order selectByOrderNo(String orderNo);

    // 查看用户所有的订单
    List<Order> selectByUserIdOrders(Long userId);

    // 查询所有订单
    List<Order> selectAll();

    // 插入订单
    int insert(Order order);

    // 更新订单
    int update(Order order);

    // 根据订单号更新订单的状态
    int updateStatusByOrderNo(@Param("orderNo") String orderNo,
                              @Param("status") Integer status);

    // 根据id删除订单，建议删除的逻辑为status=-1
    int delete(Long id);
}
