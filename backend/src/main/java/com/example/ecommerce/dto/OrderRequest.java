package com.example.ecommerce.dto;

import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.OrderItem;
import lombok.Data;

import java.util.List;

/**
 * 创建订单请求体
 */
@Data
public class OrderRequest {
    private Order order;
    private List<OrderItem> orderItems;
}
