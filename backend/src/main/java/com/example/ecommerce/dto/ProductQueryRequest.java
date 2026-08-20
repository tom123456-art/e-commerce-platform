package com.example.ecommerce.dto;

import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * 商品列表查询请求DTO
 * 分页、筛选、排序
 */
public class ProductQueryRequest {
    // 当前页码，从1开始
    private Integer page = 1;
    // 每页显示数量
    private Integer pageSize = 8;
    // 搜索关键字
    private String keyword;
    // 商品分类ID
    private Integer categoryId;
    // 最低价格和最高价格
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    // 排序字段,latest(默认),price,stock,name
    private String sortBy = "latest";
    // 排序方向:desc(降序) asc(升序)
    private String sortDirection = "desc";

    // 防御性规划
    // 获取规范化的页码
    public int getNormalizedPage() {
        return page == null || page < 1 ? 1 : page;
    }

    // 规范每页数量
    public int getNormalizedPageSize() {
        if (pageSize == null || pageSize < 1) return 8;
        return Math.min(pageSize, 50);
    }

    // 计算SQL的OFFSET值  (page-1)* pageSize
    public int getOffset() {
        return (getNormalizedPage() - 1) * getNormalizedPageSize();
    }

    // 清洗关键词的空白
    public String getSanitizedKeyword() {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    // 获取白名单校验后的排序字段
    public String getNormalizedSortBy() {
        if (!StringUtils.hasText(sortBy)) return "latest";
        String normalized = sortBy.trim().toLowerCase();
        if ("price".equals(normalized) || "stock".equals(normalized) || "name".equals(normalized)) {
            return normalized;
        }
        return "latest";
    }

    // 获取规范化排序方向
    public String getNormalizedSortDirection() {
        return "asc".equalsIgnoreCase(sortDirection) ? "asc" : "desc";
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }
}
