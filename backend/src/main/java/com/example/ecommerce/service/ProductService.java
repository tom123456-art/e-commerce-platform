package com.example.ecommerce.service;

import com.example.ecommerce.common.PagedResponse;
import com.example.ecommerce.dto.ProductQueryRequest;
import com.example.ecommerce.dto.ProductShowcaseResponse;
import com.example.ecommerce.entity.Product;

import java.util.List;

/**
 * 商品服务接口 —— 定义商品的增删改查、分页查询、热门推荐和缓存预热等功能。
 *
 * Service 层的"业务抽象"能力：
 *   - 数据聚合：商品查询涉及 Redis 缓存、热门计算、个性化推荐，Service 组合为简洁接口
 *   - 缓存策略：warmUpCache 在应用启动时预热热门商品到 Redis
 *   - 分页封装：query 返回 PagedResponse 而非裸列表，封装分页元数据
 */
public interface ProductService {

    /** 缓存预热：应用启动时将热门商品加载到 Redis，避免缓存穿透 */
    void warmUpCache();

    /**
     * 按主键查询商品。
     * 实现层采用 Cache-Aside 模式：先查 Redis，未命中再查数据库，并回写缓存。
     */
    Product getById(Long id);

    /** 查询所有商品（生产环境应改用分页查询） */
    List<Product> getAll();

    /**
     * 分页条件查询商品。
     * @param request 查询请求 DTO（含分页参数和筛选条件）
     * @return 分页响应（含商品列表和分页信息）
     */
    PagedResponse<Product> query(ProductQueryRequest request);

    /** 获取热门商品（首页展示），返回 DTO 含热度分数等额外信息 */
    List<ProductShowcaseResponse> getHotProducts(int limit);

    /** 获取个性化推荐商品（基于用户行为），未登录时降级为热门推荐 */
    List<ProductShowcaseResponse> getRecommendedProducts(Long userId, int limit);

    /** 按分类查询商品 */
    List<Product> getByCategoryId(Integer categoryId);

    /** 新增商品（同步清除相关缓存） */
    void save(Product product);

    /** 批量导入商品（Excel 导入），返回成功导入的记录数 */
    int importProducts(List<Product> products);

    /** 更新商品信息（同步清除相关缓存） */
    void update(Product product);

    /** 删除商品（同步清除相关缓存） */
    void deleteById(Long id);
}