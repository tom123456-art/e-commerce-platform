package com.example.ecommerce.service;


import com.example.ecommerce.common.PagedResponse;
import com.example.ecommerce.dto.ProductQueryRequest;
import com.example.ecommerce.dto.ProductShowcaseResponse;
import com.example.ecommerce.entity.Product;

import java.util.List;

/**
* @author waqwb
* @description 针对表【product】的数据库操作Service
* @createDate 2026-07-31 09:50:13
*/
public interface ProductService {
    /* 缓存预热，当应用启动的时候，会将热门商品加载到Redis中，避免缓存穿透 */
    void warmUpCache();

    /* 根据主键查询商品 */
    Product getById(Long id);

    /* 获取所有商品 */
    List<Product> getAll();

    /* 分页查询商品 */
    PagedResponse<Product> query(ProductQueryRequest request);

    /* 获取热门商品的展示，返回DTO包含的热度分数等其他信息 */
    List<ProductShowcaseResponse> getHotProducts(int limit);

    /* 获取个性化推荐商品，基于用户的行为，如果没有登录的时候，会降级为热门推荐 */
    List<ProductShowcaseResponse> getRecommendedProducts(Long userId, int limit);

    /* 按照分类查询商品 */
    List<Product> getByCategoryId(Integer categoryId);


    /* 新增商品 */
    void save(Product product);

    /* 更新商品 */
    void update(Product product);

    /* 删除商品 */
    void deleteById(Long id);

    /* 批量导入商品，返回成功导入的记录数 */
    int importProducts(List<Product> products);

}
