package com.example.ecommerce.mapper;

import com.example.ecommerce.entity.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
                "org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration," +
                "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"
})
@ActiveProfiles("test") // 激活application-test.yml，连接ecommerce_test数据库
@Transactional
class OrderItemMapperTest {

    @Autowired
    private OrderItemMapper orderItemMapper;

    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        orderItem = new OrderItem();
        orderItem.setOrderId(1L);
        orderItem.setProductId(1L);
        orderItem.setProductName("Product 1");
        orderItem.setPrice(new BigDecimal("19.99"));
        orderItem.setQuantity(2);
    }

    @Test
    void selectAll() {
        orderItemMapper.insert(orderItem);
        List<OrderItem> orderItems = orderItemMapper.selectAll();
        for (OrderItem orderItem : orderItems)
            System.out.println(orderItem);
    }

    @Test
    void batchInsert() {
        OrderItem orderItem1 = new OrderItem();
        orderItem1.setOrderId(1L);
        orderItem1.setProductId(2L);
        orderItem1.setProductName("Product 2");
        orderItem1.setPrice(new BigDecimal("29.99"));
        orderItem1.setQuantity(1);

        OrderItem orderItem2 = new OrderItem();
        orderItem2.setOrderId(1L);
        orderItem2.setProductId(3L);
        orderItem2.setProductName("Product 3");
        orderItem2.setPrice(new BigDecimal("39.99"));
        orderItem2.setQuantity(3);

        int i = orderItemMapper.insertBatch(Arrays.asList(orderItem1, orderItem2));
        System.out.println(i);
    }
}