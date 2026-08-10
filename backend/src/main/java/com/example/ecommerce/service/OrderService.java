package com.example.ecommerce.service;

import com.example.ecommerce.dto.OrderDetailResponse;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.OrderItem;

import java.util.List;

public interface OrderService {
    Order getById(Long id);
    OrderDetailResponse getDetailById(Long id);
    Order getByOrderNo(String orderNo);
    List<Order> getByUserId(Long userId);
    List<Order> getAll();

    /** 创建订单（事务方法）：扣库存 + 插订单 + 插明细 + 发 MQ 事件 */
    OrderDetailResponse save(Order order, List<OrderItem> orderItems);

    void update(Order order);
    void updateStatusByOrderNo(String orderNo, Integer status);
    void delete(Long id);
}
