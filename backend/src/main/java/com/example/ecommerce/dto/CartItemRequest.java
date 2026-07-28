package com.example.ecommerce.dto;

/**
 * 购物车商品操作请求 DTO（添加商品 / 修改数量）。
 *
 * 本类被两个接口共用：
 *   - POST /api/cart（添加到购物车）：前端传 productId + quantity（默认 1）
 *   - PUT /api/cart/{id}（修改数量）：前端传 productId + 新的 quantity
 * 为什么不传商品名称和价格？
 *   购物车操作只需要 productId（标识商品）和 quantity（数量），
 *   商品名称、价格、库存由后端从数据库查询。
 *   这是安全设计——如果让前端传价格，恶意用户可能篡改为 0.01 元。
 */
public class CartItemRequest {

    /**
     * 商品 ID（对应 product 表主键）。
     * 后端通过此 ID 查询商品详情、校验商品是否存在、检查库存是否充足。
     *
     * 为什么类型是 Long 而非 Integer？
     *   - Integer 最大值约 21 亿，高并发系统可能不够
     *   - Long 最大值约 9.2 × 10^18，基本不可能用尽
     *   - Long 也是 Snowflake 等分布式 ID 生成器的默认类型
     */
    private Long productId;

    /**
     * 商品数量。
     * 添加到购物车时通常为 1；修改数量时为新的目标数量（不是增量）。
     *
     * 为什么类型是 Integer 而非 int？
     *   - Integer 可以为 null，int 不行（默认为 0）
     *   - 用户不传 quantity 时，null 表示"未指定"，后端可设默认值 1
     *   - 而 0 可能被误解为"清空购物车"
     *   - DTO 字段推荐使用包装类型
     */
    private Integer quantity;

    // ========== Getter / Setter ==========
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}