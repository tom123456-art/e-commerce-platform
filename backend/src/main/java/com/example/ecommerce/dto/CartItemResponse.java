package com.example.ecommerce.dto;

import java.math.BigDecimal;

/**
 * 购物车商品项响应 DTO —— 购物车页面展示所需的全部数据。
 *
 * subtotal（小计）由后端计算而非前端：
 *   - 后端使用 BigDecimal 精确运算，避免 JavaScript 浮点数精度问题
 *   - 防止前端篡改金额
 *   - 金额相关数据统一由后端提供，保证前后端一致
 */
public class CartItemResponse {

    /** 购物车记录 ID（用于后续的删除/修改操作） */
    private Long id;

    /** 所属用户 ID */
    private Long userId;

    /** 商品 ID（用于跳转商品详情页） */
    private Long productId;

    /** 商品名称（直接展示） */
    private String productName;

    /** 商品单价（展示和计算） */
    private BigDecimal price;

    /** 商品图片 URL（展示缩略图） */
    private String image;

    /** 商品当前库存（控制数量选择器的上限） */
    private Integer stock;

    /** 用户选择的购买数量 */
    private Integer quantity;

    /** 小计金额（price × quantity），由后端计算 */
    private BigDecimal subtotal;

    // ========== Getter / Setter ==========

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}