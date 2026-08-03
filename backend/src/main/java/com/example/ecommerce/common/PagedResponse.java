package com.example.ecommerce.common;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * ============================================================
 * 【教学重点】分页响应封装类 —— 大数据量的优雅解决方案
 * ============================================================
 *
 * <h2>1. 为什么需要分页？</h2>
 * <p>假设电商平台有 10000 件商品，如果一次性全部返回：</p>
 * <ul>
 *   <li><b>网络传输</b>：JSON 数据可能达到几 MB，加载缓慢</li>
 *   <li><b>前端渲染</b>：DOM 节点过多，页面卡顿</li>
 *   <li><b>数据库压力</b>：一次查询大量数据，占用大量内存和 CPU</li>
 * </ul>
 * <p>分页的本质是：<strong>每次只请求和展示一小部分数据</strong>，
 * 用户通过翻页来浏览更多内容。</p>
 *
 * <h2>2. 分页响应的结构</h2>
 * <pre>{@code
 * {
 *   "records": [ ... ],     // 当前页的数据列表
 *   "total": 10000,         // 总记录数（用于计算总页数）
 *   "page": 1,              // 当前页码（从 1 开始）
 *   "pageSize": 20,         // 每页大小
 *   "totalPages": 500       // 总页数 = ceil(total / pageSize)
 * }
 * }</pre>
 * <p>前端拿到这个数据后，可以：</p>
 * <ul>
 *   <li>用 {@code records} 渲染当前页的数据列表</li>
 *   <li>用 {@code total} 和 {@code totalPages} 渲染分页导航组件</li>
 *   <li>用 {@code page} 高亮当前页码</li>
 * </ul>
 *
 * <h2>3. 与 Result 的关系</h2>
 * <p>{@code PagedResponse} 通常作为 {@link Result} 的 {@code data} 字段：</p>
 * <pre>{@code
 * @GetMapping("/products")
 * public Result<PagedResponse<Product>> listProducts(
 *         @RequestParam(defaultValue = "1") int page,
 *         @RequestParam(defaultValue = "20") int pageSize) {
 *     PagedResponse<Product> paged = productService.findByPage(page, pageSize);
 *     return Result.success(paged);  // PagedResponse 作为 data 嵌套在 Result 中
 * }
 * }</pre>
 * <p>前端最终收到的 JSON：</p>
 * <pre>{@code
 * {
 *   "success": true,
 *   "code": 200,
 *   "message": "success",
 *   "data": {
 *     "records": [ ... ],
 *     "total": 10000,
 *     "page": 1,
 *     "pageSize": 20,
 *     "totalPages": 500
 *   },
 *   "timestamp": 1718700000000
 * }
 * }</pre>
 *
 * <h2>4. 设计要点</h2>
 * <ul>
 *   <li><b>总页数自动计算</b>：通过 {@code Math.ceil(total / pageSize)} 自动计算，
 *       避免前端重复计算</li>
 *   <li><b>空列表保护</b>：当 records 为 null 时，自动转为空列表，避免前端 NullPointerException</li>
 *   <li><b>静态工厂方法</b>：通过 {@link #of(List, long, int, int)} 创建，确保字段完整性</li>
 * </ul>
 *
 * @param <T> 列表中数据元素的类型
 * @author 教学示例
 * @see Result 统一响应封装，PagedResponse 通常作为其 data 字段
 */
public class PagedResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 实例字段 ====================

    /**
     * 当前页的数据列表。
     * <p>教学点：泛型 List 让这个分页响应可以用于任何类型的数据，
     * 如 {@code PagedResponse<Product>}、{@code PagedResponse<User>} 等。</p>
     */
    private List<T> records;

    /**
     * 总记录数。
     * <p>教学点：这是<strong>数据库中符合条件的总记录数</strong>，
     * 不仅仅是当前页的数量。通常通过 SQL 的 {@code COUNT(*)} 查询获得。</p>
     * <p>用途：前端用它来计算总页数、显示"共 xxx 条记录"等。</p>
     */
    private long total;

    /**
     * 当前页码（从 1 开始）。
     * <p>教学点：页码从 1 开始是业务惯例，但 MyBatis 等框架的 offset 从 0 开始，
     * 转换公式：{@code offset = (page - 1) * pageSize}</p>
     */
    private int page;

    /**
     * 每页大小（每页显示的记录数）。
     * <p>教学点：通常有默认值（如 10 或 20），前端也可以自定义。
     * 但应该设置最大值限制（如 100），防止客户端请求过大的 pageSize 导致性能问题。</p>
     */
    private int pageSize;

    /**
     * 总页数。
     * <p>教学点：计算公式为 {@code ceil(total / pageSize)}。</p>
     * <p>例如：total=105, pageSize=20，则 totalPages=ceil(105/20)=6。</p>
     * <p>在 {@link #of} 方法中自动计算，无需手动设置。</p>
     */
    private int totalPages;

    // ==================== 静态工厂方法 ====================

    /**
     * 创建分页响应对象。
     *
     * <p><b>教学点</b>：使用静态工厂方法而非构造方法，优势：</p>
     * <ul>
     *   <li>方法名 {@code of} 语义清晰，一看就知道是创建分页响应</li>
     *   <li>可以包含计算逻辑（如自动计算 totalPages）</li>
     *   <li>可以包含防御性逻辑（如 null 转空列表）</li>
     * </ul>
     *
     * <p><b>使用示例</b>：</p>
     * <pre>{@code
     * // 在 Service 层使用
     * public PagedResponse<Product> findByPage(int page, int pageSize) {
     *     long total = productMapper.countAll();           // 查询总数
     *     int offset = (page - 1) * pageSize;              // 计算偏移量
     *     List<Product> products = productMapper.findByPage(offset, pageSize);
     *     return PagedResponse.of(products, total, page, pageSize);
     * }
     * }</pre>
     *
     * @param <T>      数据元素类型
     * @param records  当前页的数据列表，如果为 null 则自动转为空列表
     * @param total    总记录数
     * @param page     当前页码（从 1 开始）
     * @param pageSize 每页大小
     * @return 初始化完成的分页响应对象
     */
    public static <T> PagedResponse<T> of(List<T> records, long total, int page, int pageSize) {
        PagedResponse<T> response = new PagedResponse<>();
        // 教学点：防御性编程 —— 避免 null 列表导致前端 NPE
        response.setRecords(records == null ? Collections.emptyList() : records);
        response.setTotal(total);
        response.setPage(page);
        response.setPageSize(pageSize);
        // 教学点：ceil 向上取整，确保有余数时多出一页
        // 例如：105 条数据，每页 20 条 -> ceil(5.25) = 6 页
        response.setTotalPages(pageSize <= 0 ? 0 : (int) Math.ceil((double) total / pageSize));
        return response;
    }

    // ==================== Getter / Setter ====================

    /**
     * 获取当前页的数据列表。
     *
     * @return 数据列表，不会返回 null（构造时已做保护）
     */
    public List<T> getRecords() {
        return records;
    }

    /**
     * 设置当前页的数据列表。
     *
     * @param records 数据列表
     */
    public void setRecords(List<T> records) {
        this.records = records;
    }

    /**
     * 获取总记录数。
     *
     * @return 符合条件的总记录数
     */
    public long getTotal() {
        return total;
    }

    /**
     * 设置总记录数。
     *
     * @param total 总记录数
     */
    public void setTotal(long total) {
        this.total = total;
    }

    /**
     * 获取当前页码。
     *
     * @return 当前页码（从 1 开始）
     */
    public int getPage() {
        return page;
    }

    /**
     * 设置当前页码。
     *
     * @param page 页码（从 1 开始）
     */
    public void setPage(int page) {
        this.page = page;
    }

    /**
     * 获取每页大小。
     *
     * @return 每页显示的记录数
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * 设置每页大小。
     *
     * @param pageSize 每页记录数
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * 获取总页数。
     *
     * @return 总页数
     */
    public int getTotalPages() {
        return totalPages;
    }

    /**
     * 设置总页数。
     * <p>通常不需要手动调用，{@link #of} 方法会自动计算。</p>
     *
     * @param totalPages 总页数
     */
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}
