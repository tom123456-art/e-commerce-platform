package com.example.ecommerce.service.impl;

import com.example.ecommerce.common.BusinessException;
import com.example.ecommerce.common.PagedResponse;
import com.example.ecommerce.common.Result;
import com.example.ecommerce.dto.ProductQueryRequest;
import com.example.ecommerce.dto.ProductShowcaseResponse;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.mapper.ProductMapper;
import com.example.ecommerce.service.ProductService;
import com.example.ecommerce.utils.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.xmlbeans.impl.xb.xsdschema.TotalDigitsDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商品服务实现类。
 *
 * @Service：标记为 Spring Service 组件，Spring 容器自动扫描注册为 Bean。
 * <p>
 * 设计要点：
 * 1. 构造器注入：所有依赖声明为 final，通过构造器注入，保证不可变性
 * 2. Cache-Aside 模式：查询时先查缓存，未命中查数据库并回写缓存
 * 3. 缓存失效：写操作（save/update/delete）后主动清除相关缓存
 * 4. 异常降级：缓存操作失败时不影响主流程，降级为直接查数据库
 */
@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    /**
     * 商品缓存 TTL（秒）：1 小时
     */
    private static final long PRODUCT_CACHE_TTL_SECONDS = 3600L;

    private final ProductMapper productMapper;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 构造器注入：Spring 推荐的依赖注入方式
     */
    public ProductServiceImpl(ProductMapper productMapper, RedisUtil redisUtil) {
        this.productMapper = productMapper;
        this.redisUtil = redisUtil;
    }

    /**
     * 缓存预热：应用启动时调用，将热门商品加载到 Redis。
     * 此方法通常在 ApplicationRunner 或 @PostConstruct 中调用。
     */
    @Override
    public void warmUpCache() {
        getAll();                              // 预热全量商品缓存
        getHotProducts(6);                     // 预热热门商品缓存
        getRecommendedProducts(null, 6);       // 预热匿名推荐缓存
        logger.info("预热商品缓存");
    }

    /**
     * 按主键查询商品（Cache-Aside 模式示例）。
     * <p>
     * Cache-Aside 流程：
     * 1. 先查 Redis 缓存，命中则直接返回
     * 2. 未命中则查数据库
     * 3. 将数据库结果回写到 Redis（设置 TTL）
     * 4. 缓存操作失败时降级为直接查数据库（不影响主流程）
     */
    @Override
    public Product getById(Long id) {
        String key = "product:" + id;
        try {
            // 第一步：查 Redis 缓存
            if (redisUtil.exists(key)) {
                // 缓存命中：反序列化 JSON 为 Product 对象
                return objectMapper.readValue(String.valueOf(redisUtil.get(key)), Product.class);
            }
            // 第二步：缓存未命中，查数据库
            Product product = productMapper.selectById(id);
            // 第三步：回写缓存（只有查询结果非空才缓存，避免缓存穿透）
            if (product != null) {
                redisUtil.set(key, objectMapper.writeValueAsString(product), PRODUCT_CACHE_TTL_SECONDS);
            }
            return product;
        } catch (Exception ex) {
            // 第四步：缓存异常降级，直接查数据库
            logger.warn("商品查询{}失败: {}", id, ex.getMessage());
            return productMapper.selectById(id);
        }
    }

    /**
     * 查询所有商品（同样采用 Cache-Aside 模式）
     */
    @Override
    public List<Product> getAll() {
        String key = "products:all";
        try {
            if (redisUtil.exists(key)) {
                return objectMapper.readValue(String.valueOf(redisUtil.get(key)),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Product.class));
            }
            List<Product> products = productMapper.selectAll();
            if (products != null) {
                redisUtil.set(key, objectMapper.writeValueAsString(products), PRODUCT_CACHE_TTL_SECONDS);
            }
            return products;
        } catch (Exception e) {
            logger.warn("商品查询失败: {}", e.getMessage());
            return productMapper.selectAll();
        }
    }

    /**
     * 分页条件查询商品。
     * <p>
     * 业务校验：minPrice 不能大于 maxPrice，否则抛出 BusinessException。
     * 参数规范化：调用 ProductQueryRequest 的 getNormalizedXxx 方法做防御性处理。
     */
    @Override
    public PagedResponse<Product> query(ProductQueryRequest request) {
        ProductQueryRequest safeRequest = request == null ? new ProductQueryRequest() : request;
        // 业务校验：价格区间合法性
        if (safeRequest.getMinPrice() != null && safeRequest.getMaxPrice() != null
                && safeRequest.getMinPrice().compareTo(safeRequest.getMaxPrice()) > 0) {
            throw new BusinessException(Result.BAD_REQUEST_CODE, "Minimum price cannot be greater than maximum price");
        }
        // 参数规范化
        safeRequest.setPage(safeRequest.getNormalizedPage());
        safeRequest.setPageSize(safeRequest.getNormalizedPageSize());
        safeRequest.setKeyword(safeRequest.getSanitizedKeyword());
        safeRequest.setSortBy(safeRequest.getNormalizedSortBy());
        safeRequest.setSortDirection(safeRequest.getNormalizedSortDirection());
        // 查询数据和总数
        List<Product> records = productMapper.selectPage(safeRequest);
        long total = productMapper.countPage(safeRequest);
        // 封装分页响应
        return PagedResponse.of(records, total, safeRequest.getNormalizedPage(), safeRequest.getNormalizedPageSize());
    }


    /**
     * 新增商品。
     * 写操作后必须清除相关缓存（products:all、products:category:*、products:showcase:*），
     * 否则前端会看到旧数据。
     */
    @Override
    public void save(Product product) {
        productMapper.insert(product);
        evictProductCaches();   // 清除相关缓存
    }

    /**
     * 批量导入商品。
     *
     * @Transactional：开启事务，保证所有插入操作原子性。 如果中途任何一条插入失败，整个事务回滚，不会出现"导入了一半"的数据不一致问题。
     */
    @Override
    @Transactional
    public int importProducts(List<Product> products) {
        if (products == null || products.isEmpty()) {
            throw new IllegalArgumentException("导入商品不能为空");
        }
        for (Product product : products) {
            productMapper.insert(product);
        }
        evictProductCaches();
        return products.size();
    }

    /**
     * 更新商品信息
     */
    @Override
    public void update(Product product) {
        productMapper.update(product);
        evictProductCaches();
    }

    /**
     * 删除商品
     */
    @Override
    public void deleteById(Long id) {
        productMapper.delete(id);
        evictProductCaches();
    }

    /**
     * 清除商品相关缓存。
     * 写操作后调用，确保前端看到最新数据。
     * 使用 try-catch 保护，缓存清除失败不影响主流程。
     */
    private void evictProductCaches() {
        try {
            redisUtil.delete("products:all");
            redisUtil.deleteByPattern("products:category:*");
            redisUtil.deleteByPattern("products:showcase:*");
        } catch (Exception e) {
            logger.warn("Failed to evict product caches: {}", e.getMessage());
        }
    }

    // 其他方法（getHotProducts、getRecommendedProducts、getByCategoryId、
    // 推荐算法、热度计算等）请参考项目源码，涉及复杂的评分模型和用户画像构建。
    @Override
    public List<ProductShowcaseResponse> getHotProducts(int limit) {
        return List.of();
    }

    @Override
    public List<ProductShowcaseResponse> getRecommendedProducts(Long userId, int limit) {
        return List.of();
    }

    @Override
    public List<Product> getByCategoryId(Integer categoryId) {
        return List.of();
    }
}