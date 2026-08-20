package com.example.ecommerce.dto;

import lombok.ToString;

import java.math.BigDecimal;

/**
 * 购物车商品项响应DTO
 * <p>
 * 我们通过聚合查询，通过用户Id查询购物车
 * select c.id, c.user_id, c.product_id, c.quantity, p.name as product_name, p.price,p.image,p.stock
 * from cart_item c join product p on c.product_id = p.id
 * where c.user_id=#{userId};
 */
@ToString
public class CartItemResponse {
    // CartItem
    private Long id;
    private Long userId;
    private Long productId;
    private Integer quantity;
    // Product
    // 商品名称，sql使用了别名
    private String productName;
    private BigDecimal price;
    private String image;
    private Integer stock;
    // 小计金额，由后端来计算的
    private BigDecimal subtotal;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}
