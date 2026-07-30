package com.example.ecommerce.mapper;

import com.example.ecommerce.entity.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;


@SpringBootTest
@ActiveProfiles("test")
@Transactional //每个测试结束之后自动回滚
class OrderItemMapperTest {
    @Autowired
    private OrderItemMapper orderItemMapper;
    private OrderItem testOrderItem;

    @BeforeEach
    void setUp() {
        testOrderItem = new OrderItem();
        testOrderItem.setOrderId(1L);
        testOrderItem.setProductId(1L);
        testOrderItem.setProductName("测试商品");
        testOrderItem.setPrice(new BigDecimal("19.99"));
        testOrderItem.setQuantity(2);
    }

    @Test
    void selectAll() {
        List<OrderItem> orderItems = orderItemMapper.selectAll();
        for (OrderItem orderItem : orderItems) {
            System.out.println("查询到的订单项: " + orderItem);
        }
    }

    @Test
    void batchInsert(){
        OrderItem orderItem1 = new OrderItem();
        orderItem1.setOrderId(1L);
        orderItem1.setProductId(2L);
        orderItem1.setProductName("测试商品2");
        orderItem1.setPrice(new BigDecimal("29.99"));
        orderItem1.setQuantity(1);

        OrderItem orderItem2 = new OrderItem();
        orderItem2.setOrderId(1L);
        orderItem2.setProductId(3L);
        orderItem2.setProductName("测试商品3");
        orderItem2.setPrice(new BigDecimal("39.99"));
        orderItem2.setQuantity(3);

        int i = orderItemMapper.insertBatch(Arrays.asList(orderItem1, orderItem2));
        System.out.println(i);
    }
}