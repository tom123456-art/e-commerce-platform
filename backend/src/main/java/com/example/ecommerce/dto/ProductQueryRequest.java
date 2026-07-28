package com.example.ecommerce.dto;

import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * 商品列表查询请求 DTO（分页 + 筛选 + 排序）。
 *
 * 本类体现了"防御性编程"思想：提供多个 getNormalizedXxx() 方法，
 * 在 DTO 层做输入规范化，让 Service 层可以放心使用：
 *   - 空值防御：page 为 null 时返回默认值 1
 *   - 范围限制：pageSize 最大不超过 50，防止一次查询过多数据
 *   - 白名单过滤：sortBy 只接受 price/stock/name/latest，防止 SQL 注入
 *   - 输入清洗：keyword 去除首尾空白
 */
public class ProductQueryRequest {

    /** 当前页码（从 1 开始），默认 1 */
    private Integer page = 1;

    /** 每页显示数量，默认 8，最大 50 */
    private Integer pageSize = 8;

    /** 搜索关键词，模糊匹配商品名称和描述 */
    private String keyword;

    /** 商品分类 ID，null 表示不按分类筛选 */
    private Integer categoryId;

    /** 最低价格，null 表示不限制 */
    private BigDecimal minPrice;

    /** 最高价格，null 表示不限制 */
    private BigDecimal maxPrice;

    /** 排序字段：latest（默认）、price、stock、name */
    private String sortBy = "latest";

    /** 排序方向：desc（默认降序）、asc（升序） */
    private String sortDirection = "desc";

    // ========== 防御性规范化方法 ==========

    /** 获取规范化页码：null 或 <1 时返回 1 */
    public int getNormalizedPage() {
        return page == null || page < 1 ? 1 : page;
    }

    /** 获取规范化每页数量：限制在 [1, 50] 范围内 */
    public int getNormalizedPageSize() {
        if (pageSize == null || pageSize < 1) return 8;
        return Math.min(pageSize, 50);
    }

    /** 计算 SQL OFFSET 值：(page-1) * pageSize，如第 3 页每页 8 条 → offset=16 */
    public int getOffset() {
        return (getNormalizedPage() - 1) * getNormalizedPageSize();
    }

    /** 获取清洗后的关键词：去首尾空白，空白时返回 null */
    public String getSanitizedKeyword() {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    /**
     * 获取白名单校验后的排序字段。
     * 只接受 price/stock/name，其他值（含恶意输入如 "1;DROP TABLE"）一律降级为 "latest"。
     * 这是防止 SQL 注入的关键设计。
     */
    public String getNormalizedSortBy() {
        if (!StringUtils.hasText(sortBy)) return "latest";
        String normalized = sortBy.trim().toLowerCase();
        if ("price".equals(normalized) || "stock".equals(normalized) || "name".equals(normalized)) {
            return normalized;
        }
        return "latest";
    }

    /** 获取规范化排序方向：只接受 "asc"，其他一律视为 "desc" */
    public String getNormalizedSortDirection() {
        return "asc".equalsIgnoreCase(sortDirection) ? "asc" : "desc";
    }

    // ========== Getter / Setter ==========
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }

    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    public String getSortDirection() { return sortDirection; }
    public void setSortDirection(String sortDirection) { this.sortDirection = sortDirection; }
}