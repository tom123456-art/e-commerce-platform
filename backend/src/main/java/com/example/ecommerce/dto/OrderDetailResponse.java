package com.example.ecommerce.dto;

import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.OrderItem;

import java.util.List;

/**
 * 订单详情响应DTO，聚合订单的主信息和商品明细
 * 将数据库中order和order_item表（一对多的两张表）展平成为一个响应对象
 * 数据库中的关系：order表 1  --> order_item表  N
 */
public class OrderDetailResponse {
    // 订单主信息，包含订单号、总金额等
    private Order order;
    // 订单商品明细列表
    private List<OrderItem> orderItemList;

    public OrderDetailResponse() {
    }

    public OrderDetailResponse(Order order, List<OrderItem> orderItemList) {
        this.order = order;
        this.orderItemList = orderItemList;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public List<OrderItem> getOrderItemList() {
        return orderItemList;
    }

    public void setOrderItemList(List<OrderItem> orderItemList) {
        this.orderItemList = orderItemList;
    }
}
