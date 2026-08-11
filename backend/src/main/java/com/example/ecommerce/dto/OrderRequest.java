package com.example.ecommerce.dto;

import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.OrderItem;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 创建订单请求体
 */
@Data
public class OrderRequest {
    @NotNull(message = "订单信息不能为空")
    private Order order;
    private List<OrderItem> orderItems;
}
