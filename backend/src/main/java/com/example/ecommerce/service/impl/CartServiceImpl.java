package com.example.ecommerce.service.impl;

import com.example.ecommerce.common.BusinessException;
import com.example.ecommerce.common.Result;
import com.example.ecommerce.dto.CartCheckoutRequest;
import com.example.ecommerce.dto.CartItemRequest;
import com.example.ecommerce.dto.CartItemResponse;
import com.example.ecommerce.dto.OrderDetailResponse;
import com.example.ecommerce.entity.CartItem;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.mapper.CartItemMapper;
import com.example.ecommerce.service.CartService;
import com.example.ecommerce.service.ProductMetricService;
import com.example.ecommerce.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    private final CartItemMapper cartItemMapper;
    private final ProductService productService;
    private final ProductMetricService productMetricService;

    public CartServiceImpl(CartItemMapper cartItemMapper, ProductService productService, ProductMetricService productMetricService) {
        this.cartItemMapper = cartItemMapper;
        this.productService = productService;
        this.productMetricService = productMetricService;
    }

    /**
     * 获取用户购物车列表（通过 JOIN 一次性获取商品展示信息，避免 N+1 查询）
     *
     * @param userId
     */
    @Override
    public List<CartItemResponse> getCart(Long userId) {
        return cartItemMapper.selectCartByUserId(userId);
    }

    /**
     * 添加商品到购物车。
     * 业务规则：校验商品状态和库存 → 已存在则累加数量 → 不存在则新增 → 记录加购指标
     *
     * @param userId
     * @param request
     */
    @Override
    @Transactional
    public void addItem(Long userId, CartItemRequest request) {
        // 第一步：校验请求参数
        validateItemRequest(request);
        // 第二步：查询商品的信息，通过Service走缓存
        Product product = productService.getById(request.getProductId());
        // 第三步：校验商品的状态和库存
        validateStock(product, request.getQuantity());
        // 第四步：查询购物车是否已经存在该商品
        CartItem existing = cartItemMapper.selectByUserAndProduct(
                userId,
                request.getProductId()
        );
        // 当购物车为空的时候，我们新增记录
        if (existing == null){
            CartItem cartItem = new CartItem();
            cartItem.setUserId(userId);
            cartItem.setProductId(request.getProductId());
            cartItem.setQuantity(request.getQuantity());
            cartItemMapper.insert(cartItem);
            // 记录加购指标
            productMetricService.recordCartAddition(
                    request.getProductId(),
                    request.getQuantity()
            );
            return;
        }
        // 当购物车不为空的时候，我们更新数量

        int newQuantity = existing.getQuantity() + request.getQuantity();
        // 再次校验合并之后的库存是否充足
        validateStock(product, newQuantity);
        // 更新购物车的数量
        cartItemMapper.updateQuantity(
                existing.getId(),
                userId,
                newQuantity
        );
        // 记录加购指标
        productMetricService.recordCartAddition(
                request.getProductId(),
                request.getQuantity()
        );

    }

    /**
     * 校验商品的状态和库存
     * 校验顺序：先校验存在性，再校验状态，最后校验库存
     * 遵循快速失败原则，会把最廉价、最可能出错的校验放在最前面，避免后面无意义的后续查询
     *
     * @param product
     * @param quantity
     */
    private void validateStock(Product product, Integer quantity) {
        if (product == null) {
            throw new BusinessException(Result.NOT_FOUND_CODE, "商品不存在");
        }
        if (product.getStatus() == null || product.getStatus() != 1) {
            throw new BusinessException(Result.NOT_FOUND_CODE, "商品已下架");
        }
        if (product.getStock() == null || product.getStock() < quantity) {
            throw new BusinessException(Result.CONFLICT_CODE, "商品库存不足");
        }
    }

    // 校验 productId 和 quantity 是否合法
    private void validateItemRequest(CartItemRequest request) {
        if (request == null || request.getProductId() == null) {
            throw new BusinessException(Result.BAD_REQUEST_CODE, "商品 ID 不能为空");
        }
        if (request.getQuantity() == null || request.getQuantity() < 1) {
            throw new BusinessException(Result.BAD_REQUEST_CODE, "购买商品的数量必须大于或等于1");
        }
    }

    /**
     * 修改购物车商品数量。
     * 业务规则：校验库存上限（不可超过商品库存）→ 覆盖更新数量（带 userId 校验防越权）
     *
     * @param userId
     * @param itemId
     * @param quantity
     */
    @Override
    public void updateItem(Long userId, Long itemId, Integer quantity) {

    }

    /**
     * 移除单个购物车商品。
     * 安全设计：带 userId 校验，防止水平越权（用户 A 删用户 B 的购物车项）
     *
     * @param userId
     * @param itemId
     */
    @Override
    public void removeItem(Long userId, Long itemId) {

    }

    /**
     * 清空用户购物车（结算成功后自动调用）
     *
     * @param userId
     */
    @Override
    public void clear(Long userId) {

    }

    /**
     * 结算下单：将购物车商品转为订单。
     * 事务边界：校验库存 → 扣库存 → 创建订单 + 订单明细 → 清空购物车，任一失败全部回滚。
     *
     * @param userId  当前用户 ID
     * @param request 结算请求（只传 addressId，后端验证地址归属，防篡改）
     * @return 订单详情（含订单号、总金额、订单项列表）
     */
    @Override
    public OrderDetailResponse checkout(Long userId, CartCheckoutRequest request) {
        return null;
    }
}
