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
import com.example.ecommerce.service.OrderService;
import com.example.ecommerce.utils.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 订单业务的实现类
 * 核心功能：事务下单(保证库存扣减和订单创建的原子性)+库存乐观锁(防超卖)
 */
@Service
public class OrderServiceImpl implements OrderService {

    // 日志
    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    // 注入
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final OrderMessagePublisher orderMessagePublisher;
    private final RedisUtil redisUtil;

    public OrderServiceImpl(OrderMapper orderMapper, OrderItemMapper orderItemMapper, ProductMapper productMapper, OrderMessagePublisher orderMessagePublisher, RedisUtil redisUtil) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
        this.orderMessagePublisher = orderMessagePublisher;
        this.redisUtil = redisUtil;
    }

    /**
     * @param id
     * @return
     */
    @Override
    public Order getById(Long id) {
        return orderMapper.selectById(id);
    }

    /**
     * @param id
     * @return
     */
    @Override
    public OrderDetailResponse getDetailById(Long id) {
        // 获取订单
        Order order = orderMapper.selectById(id);
        if (order == null)
            throw new BusinessException(Result.NOT_FOUND_CODE, "订单不存在");
        // 返回订单明细响应对象，调用的是全参构造函数
        return new OrderDetailResponse(order, orderItemMapper.selectByOrderId(id));
    }

    /**
     * @param orderNo
     * @return
     */
    @Override
    public Order getByOrderNo(String orderNo) {
        return orderMapper.selectByOrderNo(orderNo);
    }

    /**
     * @param userId
     * @return
     */
    @Override
    public List<Order> getByUserId(Long userId) {
        return orderMapper.selectByUserIdOrders(userId);
    }

    /**
     * @return
     */
    @Override
    public List<Order> getAll() {
        return orderMapper.selectAll();
    }

    /**
     * 创建订单（事务方法）：扣库存 + 插订单 + 插明细 + 发 MQ 事件
     *
     * @param order
     * @param orderItems
     */
    @Override
    @Transactional
    public OrderDetailResponse save(Order order, List<OrderItem> orderItems) {
        // 一、参数校验
        if (order == null)
            throw new BusinessException(Result.BAD_REQUEST_CODE, "订单信息不能为空");
        List<OrderItem> safeItems
                = orderItems == null ? Collections.emptyList() : orderItems;
        if (safeItems.isEmpty())
            throw new BusinessException(Result.BAD_REQUEST_CODE, "订单明细不能为空");
        // 二、生成订单号，使用UUID
        if (order.getOrderNo() == null || order.getOrderNo().trim().isEmpty())
            // 去除UUID生成的字符串中的"-"符号
            order.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
        // 三、检查是否存在重复订单号
        // 通过订单号获取订单信息
        Order existing = orderMapper.selectByOrderNo(order.getOrderNo());
        if (existing != null){
            log.warn("订单号{}已存在，返回已有订单详情", order.getOrderNo());
            return getDetailById(existing.getId());
        }
        // 四、订单默认状态为待支付
        if (order.getStatus() == null)
            // 设置订单状态为待支付
            order.setStatus(0);
        // 五、遍历订单项 --> 校验商品  --> 扣库存 --> 获取价格快照 --> 计算累加金额
        // 设置订单总金额为0
        BigDecimal totalAmount = BigDecimal.ZERO;
        // 遍历订单项
        for (OrderItem orderItem : safeItems){
            // TODO:校验和扣除库存（原子操作）
            Product product = validateAndDecreaseStock(orderItem);
            // 价格快照
            orderItem.setProductName(product.getName());
            orderItem.setPrice(product.getPrice());
            totalAmount = totalAmount.add(
                    product.getPrice().multiply(
                            BigDecimal.valueOf(orderItem.getQuantity())
                    )
            );
            // 缓存与事务一致性
            // 在事务提交前清缓存，如果事务回滚，库存已回滚，但缓存已清空
            // 可能导致短暂缓存未命中，一般我们要在事务提交之后清除缓存
            clearProductCache(product);
        }
        order.setTotalAmount(totalAmount);
        // 六、插入订单表
        orderMapper.insert(order);
        // 七、把生成的orderId回填到每个订单项，再批量插入
        for (OrderItem item: safeItems){
            item.setOrderId(order.getId());
        }
        orderItemMapper.insertBatch(safeItems);
        // 八、发布“订单创建”到MQ
        orderMessagePublisher.publishOrderCreated(order, safeItems);
        // 九、返回订单详情
        return new OrderDetailResponse(order, safeItems);
    }

    /**
     * 清理商品相关的Redis缓存，当库存变化以后，旧的缓存失效
     * @param product
     */
    private void clearProductCache(Product product) {
        try {
            redisUtil.delete("product:" + product.getId());
            redisUtil.delete("products:all");
            if (product.getCategoryId() != null){
                redisUtil.delete("product:category:" + product.getCategoryId());
            }
            clearShowcaseCache();
        } catch (Exception e) {
            log.warn("清理商品缓存出错：{}", e.getMessage());
        }
    }

    /**
     * 清除首页展示商品缓存
     */
    private void clearShowcaseCache() {
        try {
            redisUtil.deleteByPattern("products:showcase:*");
        } catch (Exception e) {
            log.warn("清除首页展示商品缓存出错：{}", e.getMessage());
        }
    }

    /**
     * 校验商品和扣除库存（原子操作），防止超卖的核心逻辑
     * update product set stock = stock - #{qty}
     * where id=#{id} and stock >= #{qty} and status =1
     * @param orderItem
     */
    private Product validateAndDecreaseStock(OrderItem orderItem) {
        // 判断商品信息是否存在
        if (orderItem.getProductId() == null ||
                orderItem.getQuantity() == null ||
                orderItem.getQuantity() <= 0){
            throw new BusinessException(Result.BAD_REQUEST_CODE,
                    "商品和商品数量不能为空");
        }
        // 获取商品详细信息
        Product product = productMapper.selectById(orderItem.getProductId());
        if (product == null || product.getStatus() == null || product.getStatus() != 1){
            throw new BusinessException(Result.NOT_FOUND_CODE, "商品不存在或已下架");
        }
        // 扣减库存
        int i = productMapper.decreaseStock(orderItem.getProductId(), orderItem.getQuantity());
        if (i != 1){
            throw new BusinessException(Result.CONFLICT_CODE, "库存不足");
        }
        return product;
    }

    /**
     * @param order
     */
    @Override
    public void update(Order order) {
        orderMapper.update(order);
        clearShowcaseCache();
    }

    /**
     * @param orderNo
     * @param status
     */
    @Override
    public void updateStatusByOrderNo(String orderNo, Integer status) {
        orderMapper.updateStatusByOrderNo(orderNo, status);
        clearShowcaseCache();
    }

    /**
     * @param id
     */
    @Override
    @Transactional
    public void delete(Long id) {
        // [FIX-3] 软删除：仅将订单状态置为 -1（已删除），不再物理删除订单及其明细，
        // 以保留财务/审计所需的订单与订单项数据。
        orderMapper.delete(id);
        clearShowcaseCache();
    }
}
