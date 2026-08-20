package com.example.ecommerce.mapper;

import com.example.ecommerce.entity.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
                "org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration," +
                "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"
})
@ActiveProfiles("test") // 激活application-test.yml，连接ecommerce_test数据库
@Transactional
class OrderMapperTest {

    @Autowired
    private OrderMapper orderMapper;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = new Order();
        testOrder.setUserId(1L);
        testOrder.setOrderNo("TEST" + System.currentTimeMillis());
        testOrder.setTotalAmount(new BigDecimal("100.00"));
        testOrder.setStatus(1);
        testOrder.setAddress("Shanghai");
        testOrder.setPhone("12345678901");
        testOrder.setReceiver("张三");
    }

    @Test
    void selectById() {
        int i = orderMapper.insert(testOrder);
        assertNotNull(i);
        assertEquals(1, i);
        if (i > 0) {
            Order order = orderMapper.selectById(testOrder.getId());
            System.out.println(order);
        }
    }

    @Test
    void selectByOrderNo() {
        int i = orderMapper.insert(testOrder);
        assertNotNull(i);
        assertEquals(1, i);
        if (i > 0) {
            Order order = orderMapper.selectByOrderNo(testOrder.getOrderNo());
            System.out.println(order);
        }
    }

    @Test
    void selectByUserIdOrders() {
        int i = orderMapper.insert(testOrder);
        assertNotNull(i);
        assertEquals(1, i);
        if (i > 0) {
            List<Order> orders = orderMapper.selectByUserIdOrders(testOrder.getUserId());
            for (Order order : orders)
                System.out.println(order);
        }
    }

    @Test
    void insert() {
        int i = orderMapper.insert(testOrder);
        assertNotNull(i);
        assertEquals(1, i);
    }

    @Test
    void selectAll() {
        List<Order> orders = orderMapper.selectAll();
        for (Order order : orders)
            System.out.println(order);
    }

    @Test
    void update() {
        int i = orderMapper.insert(testOrder);
        assertNotNull(i);
        assertEquals(1, i);
        if (i > 0) {
            Order order = orderMapper.selectById(testOrder.getId());
            order.setStatus(2);
            int j = orderMapper.update(order);
            assertNotNull(j);
            assertEquals(1, j);
        }
    }

    @Test
    void delete() {
        int i = orderMapper.insert(testOrder);
        assertNotNull(i);
        assertEquals(1, i);
        if (i > 0) {
            int j = orderMapper.delete(testOrder.getId());
            assertNotNull(j);
            assertEquals(1, j);
        }
    }

}