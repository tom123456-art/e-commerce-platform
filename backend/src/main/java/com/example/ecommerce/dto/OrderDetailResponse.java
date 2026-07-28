package com.example.ecommerce.dto;

import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.OrderItem;

import java.util.List;

/**
 *订单详情响应DTO，聚合订单的主信息和商品明细
 * 将数据库中order和order——item表（一对多的两张表）站平成为一个响应对象
 *
 */
public class OrderDetailResponse {

    private Order order;

    private List<OrderItem> orderItems;

    public OrderDetailResponse() {
    }

    public OrderDetailResponse(Order order, List<OrderItem> orderItems) {
        this.order = order;
        this.orderItems = orderItems;
    }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public List<OrderItem> getOrderItems() { return orderItems; }
    public void setOrderItems(List<OrderItem> orderItems) { this.orderItems = orderItems; }
}