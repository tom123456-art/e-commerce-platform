package com.example.ecommerce.dto;

import java.math.BigDecimal;

/**
 * 商品详情响应 DTO
 * 使用 Product 中需要的字段来定制响应结构，避免直接暴露 Product 实体
 */
public class ProductDetailResponse {

    /**
     * 商品ID
     */
    private Long id;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 商品价格
     */
    private BigDecimal price;

    /**
     * 评论数量
     */
    private Integer commentCount;

    /**
     * 平均评分
     */
    private Double averageRating;

    public ProductDetailResponse() {
    }

    public ProductDetailResponse(Long id, String name, String description,
                                 BigDecimal price, Integer commentCount, Double averageRating) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.commentCount = commentCount;
        this.averageRating = averageRating;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }
}