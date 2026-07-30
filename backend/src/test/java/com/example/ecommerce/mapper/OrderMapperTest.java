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


@SpringBootTest
@ActiveProfiles("test")
@Transactional //每个测试结束之后自动回滚
class OrderMapperTest {
    @Autowired
    private OrderMapper orderMapper;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = new Order();
        testOrder.setUserId(1L);
        testOrder.setOrderNo("TEST"+System.currentTimeMillis());
        testOrder.setTotalAmount(new BigDecimal("100.00"));
        testOrder.setStatus(1);
        testOrder.setAddress("测试地址");
        testOrder.setPhone("12345678901");
        testOrder.setReceiver("测试用户");
    }

    @Test
    void selectById(){
        int i = orderMapper.insert(testOrder);
        assertNotNull(i);
        assertEquals(1,i);
        if (i > 0){
            Order order = orderMapper.selectById(testOrder.getId());
            System.out.println("查询到的订单: " + order);
        }
    }

    @Test
    void selectByOrderNo(){
        int i = orderMapper.insert(testOrder);
        assertNotNull(i);
        assertEquals(1,i);
        if (i > 0){
            Order order = orderMapper.selectByOrderNo(testOrder.getOrderNo());
            System.out.println("查询到的订单: " + order);
        }
    }

    @Test
    void selectByUserIdOrders(){
        int i = orderMapper.insert(testOrder);
        assertNotNull(i);
        assertEquals(1,i);
        if (i > 0){
            List<Order> orders = orderMapper.selectByUserIdOrders(testOrder.getUserId());
            for (Order order : orders)
                System.out.println("查询到的订单: " + order);
        }
    }

    @Test
    void insert(){
        int i = orderMapper.insert(testOrder);
        assertNotNull(i);
        assertEquals(1,i);
    }

    @Test
    void update(){
        int i = orderMapper.insert(testOrder);
        assertNotNull(i);
        assertEquals(1,i);
        if (i > 0){
            testOrder.setStatus(0);
            int rows = orderMapper.update(testOrder);
            if (rows > 0){
                System.out.println("订单更新成功");
                Order order = orderMapper.selectById(testOrder.getId());
                System.out.println(order);
            } else {
                System.out.println("订单更新失败");
            }
        }
    }

    @Test
    void delete(){
        int i = orderMapper.insert(testOrder);
        assertNotNull(i);
        assertEquals(1,i);
        if(i > 0){
            int j = orderMapper.delete(testOrder.getId());
            assertNotNull(j);
            assertEquals(1,j);
        }
    }
}