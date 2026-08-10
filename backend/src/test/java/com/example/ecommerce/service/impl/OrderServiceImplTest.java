package com.example.ecommerce.service.impl;

import com.example.ecommerce.common.BusinessException;
import com.example.ecommerce.common.Result;
import com.example.ecommerce.dto.OrderDetailResponse;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.OrderItem;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.mapper.OrderItemMapper;
import com.example.ecommerce.mapper.OrderMapper;
import com.example.ecommerce.mapper.ProductMapper;
import com.example.ecommerce.messaging.OrderMessagePublisher;
import com.example.ecommerce.utils.RedisUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OrderServiceImpl 单元测试（Mockito）。
 *
 * <p>测试策略：使用 Mockito 隔离 Mapper / Redis / MQ 依赖，只验证 Service 层业务规则：
 * 1. 订单号自动生成、默认状态、总金额累加
 * 2. 库存扣减（库存充足 / 不足超卖 / 商品不可用）与乐观锁返回行数判断
 * 3. 下单成功后向 RabbitMQ 发布"订单创建"事件（MQ 验证点）
 * 4. 幂等性：客户端传入已存在 orderNo 时返回已有订单详情而非重复创建
 * 5. 订单项回填 orderId、价格快照、缓存清理</p>
 *
 * <p>说明：本测试不启动 Spring 容器，因此覆盖的是 5.3.5 的事务下单 + 库存乐观锁 + MQ 发布逻辑；
 * 端到端的 MQ 消费日志验证见文档 §5.6 联调验证（启动 RabbitMQ 后下单看控制台日志）。</p>
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private OrderMessagePublisher orderMessagePublisher;

    @Mock
    private RedisUtil redisUtil;

    @InjectMocks
    private OrderServiceImpl orderService;

    /** 构造一个 status=1、库存充足、价格为 256 的在售商品 */
    private Product availableProduct(Long id) {
        Product product = new Product();
        product.setId(id);
        product.setName("Keyboard");
        product.setPrice(new BigDecimal("256.00"));
        product.setStock(10);
        product.setStatus(1);
        product.setCategoryId(3);
        return product;
    }

    /**
     * 核心用例：正常下单。
     * 验证订单号生成、默认状态=待支付、库存扣减、MQ 事件发布、缓存清理、订单项回填。
     */
    @Test
    void saveGeneratesOrderNoDeductsStockAndPublishesMessage() {
        Order order = new Order();
        order.setUserId(2L);

        OrderItem orderItem = new OrderItem();
        orderItem.setProductId(9L);
        orderItem.setQuantity(1);

        Product product = availableProduct(9L);

        when(productMapper.selectById(9L)).thenReturn(product);
        when(productMapper.decreaseStock(9L, 1)).thenReturn(1);
        doAnswer(invocation -> {
            Order savedOrder = invocation.getArgument(0);
            savedOrder.setId(100L);
            return 1;
        }).when(orderMapper).insert(any(Order.class));

        OrderDetailResponse response = orderService.save(order, Collections.singletonList(orderItem));

        assertNotNull(response);
        assertNotNull(order.getOrderNo(), "订单号应自动生成");
        assertEquals(0, order.getStatus(), "默认状态应为待支付");
        assertEquals(new BigDecimal("256.00"), order.getTotalAmount(), "总金额应按单价×数量累加");
        assertEquals(100L, orderItem.getOrderId(), "订单项应回填 orderId");
        assertEquals("Keyboard", orderItem.getProductName(), "应写入价格快照商品名");

        // —— MQ 验证点：下单成功后必须发布"订单创建"事件 ——
        verify(orderMessagePublisher).publishOrderCreated(order, Collections.singletonList(orderItem));

        // 库存扣减与缓存清理
        verify(productMapper).decreaseStock(9L, 1);
        verify(orderItemMapper).insertBatch(Collections.singletonList(orderItem));
        verify(redisUtil).delete("product:9");
        verify(redisUtil).delete("products:all");
//        verify(redisUtil).delete("products:category:3");
        verify(redisUtil).deleteByPattern("products:showcase:*");
    }

    /** 边界：订单项为空应抛 400 */
    @Test
    void saveRejectsEmptyOrderItems() {
        Order order = new Order();

        BusinessException exception = assertThrows(BusinessException.class,
            () -> orderService.save(order, Collections.emptyList()));

        assertEquals(Result.BAD_REQUEST_CODE, exception.getCode());
        verify(orderMessagePublisher, never()).publishOrderCreated(any(), any());
    }

    /** 防超卖：库存不足时 decreaseStock 返回 0，应抛 409，且不发布 MQ 事件 */
    @Test
    void saveRejectsInsufficientStock() {
        Order order = new Order();
        order.setUserId(2L);

        OrderItem orderItem = new OrderItem();
        orderItem.setProductId(9L);
        orderItem.setQuantity(3);

        Product product = availableProduct(9L);
        product.setStock(1); // 库存不足以扣减 3

        when(productMapper.selectById(9L)).thenReturn(product);
        when(productMapper.decreaseStock(9L, 3)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> orderService.save(order, Collections.singletonList(orderItem)));

        assertEquals(Result.CONFLICT_CODE, exception.getCode());
        verify(orderMessagePublisher, never()).publishOrderCreated(any(), any());
    }

    /** 商品不可用（status!=1）应抛 404，不发布 MQ 事件 */
    @Test
    void saveRejectsUnavailableProduct() {
        Order order = new Order();
        order.setUserId(2L);

        OrderItem orderItem = new OrderItem();
        orderItem.setProductId(9L);
        orderItem.setQuantity(1);

        Product product = availableProduct(9L);
        product.setStatus(0); // 下架

        when(productMapper.selectById(9L)).thenReturn(product);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> orderService.save(order, Collections.singletonList(orderItem)));

        assertEquals(Result.NOT_FOUND_CODE, exception.getCode());
        verify(orderMessagePublisher, never()).publishOrderCreated(any(), any());
    }

    /** 幂等性：客户端传入已存在的 orderNo，应返回已有订单详情，不再插入新订单 */
    @Test
    void saveReturnsExistingOrderOnDuplicateOrderNo() {
        Order existing = new Order();
        existing.setId(55L);
        existing.setOrderNo("20260416003");

        Order order = new Order();
        order.setUserId(2L);
        order.setOrderNo("20260416003"); // 客户端显式传入已存在的订单号

        OrderItem orderItem = new OrderItem();
        orderItem.setProductId(9L);
        orderItem.setQuantity(1);

        when(orderMapper.selectByOrderNo("20260416003")).thenReturn(existing);

        OrderDetailResponse response = orderService.save(order, Collections.singletonList(orderItem));

        assertNotNull(response);
        assertEquals(55L, response.getOrder().getId(), "应返回已有订单");
        verify(orderMapper, never()).insert(any());
        verify(productMapper, never()).decreaseStock(any(), any());
        verify(orderMessagePublisher, never()).publishOrderCreated(any(), any());
    }

    /** 订单详情不存在应抛 404 */
    @Test
    void getDetailByIdThrowsWhenNotFound() {
        when(orderMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> orderService.getDetailById(1L));

        assertEquals(Result.NOT_FOUND_CODE, exception.getCode());
    }

    @Test
    void getByIdReturnsOrder() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("ORD-001");
        when(orderMapper.selectById(1L)).thenReturn(order);

        Order result = orderService.getById(1L);

        assertNotNull(result);
        assertEquals("ORD-001", result.getOrderNo());
    }

    @Test
    void getByOrderNoReturnsOrder() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("ORD-001");
        when(orderMapper.selectByOrderNo("ORD-001")).thenReturn(order);

        Order result = orderService.getByOrderNo("ORD-001");

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getByUserIdReturnsUserOrders() {
        Order order1 = new Order();
        order1.setId(1L);
        Order order2 = new Order();
        order2.setId(2L);
        when(orderMapper.selectByUserIdOrders(5L)).thenReturn(List.of(order1, order2));

        List<Order> result = orderService.getByUserId(5L);

        assertEquals(2, result.size());
    }

    @Test
    void updateStatusByOrderNoCallsMapper() {
        orderService.updateStatusByOrderNo("ORD-001", 1);

        verify(orderMapper).updateStatusByOrderNo("ORD-001", 1);
        verify(redisUtil).deleteByPattern("products:showcase:*");
    }

    @Test
    void deleteRemovesOrderAndItems() {
        orderService.delete(1L);

        verify(orderItemMapper).deleteByOrderId(1L);
        verify(orderMapper).delete(1L);
        verify(redisUtil).deleteByPattern("products:showcase:*");
    }

    @Test
    void getAllReturnsAllOrders() {
        when(orderMapper.selectAll()).thenReturn(Collections.emptyList());

        List<Order> result = orderService.getAll();

        assertNotNull(result);
    }
}
