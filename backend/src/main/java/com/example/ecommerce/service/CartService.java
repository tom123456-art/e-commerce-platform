package com.example.ecommerce.service;

import com.example.ecommerce.dto.CartCheckoutRequest;
import com.example.ecommerce.dto.CartItemRequest;
import com.example.ecommerce.dto.CartItemResponse;
import com.example.ecommerce.dto.OrderDetailResponse;

import java.util.List;

/**
 * 购物车服务接口 —— 承上启下的关键环节。
 * <p>
 * 向上：为 Controller 提供简洁方法（getCart / addItem / updateItem / removeItem / clear / checkout）
 * 向下：编排 CartItem、Product、Order 多张表
 * <p>
 * 事务一致性：
 * - addItem 涉及"查商品 + 新增/更新购物车"两步，必须在同一事务中完成。
 * - checkout 涉及"创建订单 + 清空购物车"两步，必须在同一事务中完成。
 */
public interface CartService {

    /**
     * 获取用户购物车列表（通过 JOIN 一次性获取商品展示信息，避免 N+1 查询）
     */
    List<CartItemResponse> getCart(Long userId);

    /**
     * 添加商品到购物车。
     * 业务规则：校验商品状态和库存 → 已存在则累加数量 → 不存在则新增 → 记录加购指标
     */
    void addItem(Long userId, CartItemRequest request);

    /**
     * 修改购物车商品数量。
     * 业务规则：校验库存上限（不可超过商品库存）→ 覆盖更新数量（带 userId 校验防越权）
     */
    void updateItem(Long userId, Long itemId, Integer quantity);

    /**
     * 移除单个购物车商品。
     * 安全设计：带 userId 校验，防止水平越权（用户 A 删用户 B 的购物车项）
     */
    void removeItem(Long userId, Long itemId);

    /**
     * 清空用户购物车（结算成功后自动调用）
     */
    void clear(Long userId);

    /**
     * 结算下单：将购物车商品转为订单。
     * 事务边界：校验库存 → 扣库存 → 创建订单 + 订单明细 → 清空购物车，任一失败全部回滚。
     *
     * @param userId  当前用户 ID
     * @param request 结算请求（只传 addressId，后端验证地址归属，防篡改）
     * @return 订单详情（含订单号、总金额、订单项列表）
     */
    OrderDetailResponse checkout(Long userId, CartCheckoutRequest request);
}
