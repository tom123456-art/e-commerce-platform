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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    /* 商品缓存TTL秒 1小时 */
    private static final long PRODUCT_CACHE_TTL_SECONDS = 60 * 60L;

    // 分类标签映射，帮我显示中文分类名
    private static final Map<Integer, String> CATEGORY_LABELS = createCategoryLabels();
    private final ProductMapper productMapper;
    private final RedisUtil redisUtil;
    /* JSON工具 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /* 构造器注入 */
    public ProductServiceImpl(ProductMapper productMapper, RedisUtil redisUtil) {
        this.productMapper = productMapper;
        this.redisUtil = redisUtil;
    }

    private static Map<Integer, String> createCategoryLabels() {
        Map<Integer, String> labels = new HashMap<>();
        labels.put(1, "手机数码");
        labels.put(2, "电脑办公");
        labels.put(3, "智能家电");
        labels.put(4, "家居生活");
        labels.put(5, "运动户外");
        labels.put(6, "影音娱乐");
        return labels;
    }

    /**
     * 缓存预热，在启动的时候，加载所有的热门商品到Redis缓存中，避免缓存穿透
     */
    @Override
    public void warmUpCache() {
        getAll(); // 预热全量商品缓存
        getHotProducts(6); // 预热热门商品缓存
        getRecommendedProducts(null, 6); // 预热匿名推荐缓存
        logger.info("预热商品缓存");
    }

    /**
     * 按照主键查询商品
     * 使用Cache Aside模式，
     * 1、先从Redis缓存中查询，
     * 如果没有命中，再从数据库中查询，并将结果写入缓存，设置TTL
     * 命中的话就会直接返回
     * 2、缓存操作失败的时候会降级为直接查询数据库，这样不影响主流程
     *
     * @param id
     * @return
     */
    @Override
    public Product getById(Long id) {
        // /product/{id}
        String key = "product:" + id;
        try {
            // 查询缓存
            if (redisUtil.exists(key)) {
                // 如果存在，反序列化JSON为Product对象
                return objectMapper.readValue(
                        String.valueOf(redisUtil.get(key)),
                        Product.class);
            }
            // 如果缓存未命中，查询数据库
            Product product = productMapper.selectById(id);
            // 写入缓存，只有查询的结果是非空的时候才缓存，避免缓存穿透
            if (product != null) {
                redisUtil.set(
                        key,
                        objectMapper.writeValueAsString(product),
                        PRODUCT_CACHE_TTL_SECONDS
                );
            }
            return product;
        } catch (Exception e) {
            logger.warn("商品查询{}失败：{}", id, e.getMessage());
            // 直接查询数据库
            return productMapper.selectById(id);
        }
    }

    /**
     * @return
     */
    @Override
    public List<Product> getAll() {
        String key = "products:all";

        try {
            if (redisUtil.exists(key)) {
                return objectMapper.readValue(
                        String.valueOf(redisUtil.get(key)),
                        objectMapper.getTypeFactory().constructCollectionType(
                                List.class,
                                Product.class
                        )
                );
            }
            List<Product> products = productMapper.selectAll();
            if (products != null) {
                redisUtil.set(
                        key,
                        objectMapper.writeValueAsString(products),
                        PRODUCT_CACHE_TTL_SECONDS
                );
            }
            return products;
        } catch (Exception e) {
            logger.warn("商品查询失败：{}", e.getMessage());
            return productMapper.selectAll();
        }
    }

    /**
     * 分页条件查询商品
     *
     * @param request
     * @return
     */
    @Override
    public PagedResponse<Product> query(ProductQueryRequest request) {
        ProductQueryRequest safeRequest =
                request == null ? new ProductQueryRequest() : request;
        if (safeRequest.getMinPrice() != null && safeRequest.getMaxPrice() != null
                && safeRequest.getMinPrice().compareTo(safeRequest.getMaxPrice()) > 0) {
            throw new BusinessException(Result.BAD_REQUEST_CODE, "价格范围设置错误");
        }
        // 设置参数
        safeRequest.setPage(safeRequest.getNormalizedPage());
        safeRequest.setPageSize(safeRequest.getNormalizedPageSize());
        safeRequest.setKeyword(safeRequest.getSanitizedKeyword());
        safeRequest.setSortBy(safeRequest.getNormalizedSortBy());
        safeRequest.setSortDirection(safeRequest.getNormalizedSortDirection());
        // 查询数据和总数
        List<Product> records = productMapper.selectPage(safeRequest);
        long total = productMapper.countPage(safeRequest);
        return PagedResponse.of(records, total,
                safeRequest.getNormalizedPage(), safeRequest.getNormalizedPageSize());
    }

    /**
     * @param limit
     * @return
     */
    @Override
    public List<ProductShowcaseResponse> getHotProducts(int limit) {
        return List.of();
    }

    /**
     * @param userId
     * @param limit
     * @return
     */
    @Override
    public List<ProductShowcaseResponse> getRecommendedProducts(Long userId, int limit) {
        return List.of();
    }

    /**
     * @param categoryId
     * @return
     */
    @Override
    public List<Product> getByCategoryId(Integer categoryId) {
        return List.of();
    }

    /**
     * @param product
     */
    @Override
    public void save(Product product) {
        productMapper.insert(product);
        /* 缓存清除 */
        evictProductCaches();
    }

    /**
     * 清除商品相关的缓存，写操作后调用，确保前端看到的是最新的数据
     * 使用try catch保护，如果缓存清理失败的话，不会影响主流程
     */
    private void evictProductCaches() {
        try {
            redisUtil.delete("products:all");
            redisUtil.deleteByPattern("products:category:*");
            redisUtil.deleteByPattern("products:showcase:*");
        } catch (Exception e) {
            logger.warn("商品缓存清理失败：{}", e.getMessage());
        }
    }

    /**
     * @param product
     */
    @Override
    public void update(Product product) {
        // 先查询是否存在，然后再更新
        Long productId = product == null ? null : product.getId();
        // 查询缓存，如果缓存中存在，则说明商品存在，否则可能不存在
        Product previous =
                productId == null ? null : productMapper.selectById(productId);
        // 商品字段的校验和规范化处理
        Product normalizedProduct = normalizeProduct(product, true);
        productMapper.update(normalizedProduct);
        evictProductCaches();

    }

    /**
     * 商品字段的规范化处理
     *
     * @param product
     * @param requiredId 是否要求商品ID必须存在
     * @return
     */
    private Product normalizeProduct(Product product, boolean requiredId) {
        if (product == null)
            throw new IllegalArgumentException("商品信息不能为空");
        if (requiredId && product.getId() == null)
            throw new IllegalArgumentException("商品ID不能为空");
        Product normalizedProduct = new Product();
        normalizedProduct.setId(product.getId());
        normalizedProduct.setName(
                requireText(product.getName(), "商品名称不能为空"));
        normalizedProduct.setDescription(optionalText(product.getDescription()));
        normalizedProduct.setPrice(normalizedPrice(product.getPrice()));
        normalizedProduct.setStock(normalizeStock(product.getStock()));
        normalizedProduct.setImage(optionalText(product.getImage()));
        normalizedProduct.setCategoryId(normalizeCategoryId(product.getCategoryId()));
        normalizedProduct.setStatus(normalizeStatus(product.getStatus()));
        return normalizedProduct;
    }


    /**
     * 库存规范化：非空、非负
     */
    private Integer normalizeStock(Integer stock) {
        if (stock == null) {
            throw new IllegalArgumentException("商品库存不能为空");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("商品库存不能小于 0");
        }
        return stock;
    }

    /**
     * 分类合法性校验：必须在预定义的 6 个分类内
     */
    private Integer normalizeCategoryId(Integer categoryId) {
        if (categoryId == null || !CATEGORY_LABELS.containsKey(categoryId)) {
            throw new IllegalArgumentException("商品分类无效");
        }
        return categoryId;
    }

    /**
     * 状态规范化：null 时默认 1（上架），只允许 0/1
     */
    private Integer normalizeStatus(Integer status) {
        int normalized = status == null ? 1 : status;
        if (normalized != 0 && normalized != 1) {
            throw new IllegalArgumentException("商品状态无效");
        }
        return normalized;
    }

    private BigDecimal normalizedPrice(BigDecimal price) {
        if (price == null)
            throw new IllegalArgumentException("商品价格不能为空");
        if (price.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("商品价格不能为负");
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    private String requireText(String name, String message) {
        String normalized = optionalText(name);
        if (normalized == null || normalized.isEmpty())
            throw new IllegalArgumentException(message);
        return normalized;
    }

    private String optionalText(String name) {
        if (name == null) return null;
        String normalized = name.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * @param id
     */
    @Override
    public void deleteById(Long id) {
        productMapper.delete(id);
        evictProductCaches();
    }

    /**
     * 批量导入商品
     * Transactional开启事务，保证所有的插入具有原子性
     *
     * @param products
     * @return
     */
    @Override
    @Transactional
    public int importProducts(List<Product> products) {
        if (products == null || products.isEmpty()) {
            throw new IllegalArgumentException("导入商品不能为空");
        }
        for (Product p : products) {
            productMapper.insert(p);
        }
        evictProductCaches();
        return products.size();
    }
}
