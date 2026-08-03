package com.example.ecommerce.controller;

import com.example.ecommerce.common.PagedResponse;
import com.example.ecommerce.common.Result;
import com.example.ecommerce.dto.ProductQueryRequest;
import com.example.ecommerce.dto.ProductShowcaseResponse;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.service.ProductMetricService;
import com.example.ecommerce.service.ProductService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品管理控制器，处理商品的CRUD、查询、导入导出
 * @Tag :Swagger文档分组标签，在SwaggerUI中按照标签分类展示接口
 */
@Tag(name="商品接口", description = "商品查询与管理")
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final ProductMetricService productMetricService;

    /* 构造器注入，final字段保证依赖不可变 */
    public ProductController(ProductService productService, ProductMetricService productMetricService) {
        this.productService = productService;
        this.productMetricService = productMetricService;
    }

    /**
     * 根据id获取商品
     *
     * @param id             商品id，@PathVariable：从URL路径中提取{id}变量，自动转换为Long
     * @param userId         用户ID
     * @return
     */
    @GetMapping("/{id}")
    public Result<Product> getById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        // 1、调用Service查询商品
        Product product = productService.getById(id);
        // 2、判断如果不是管理员访问，那么记录商品的浏览量，为后续做数据分析使用
        if (product != null) {
            productMetricService.recordProductView(
                    id,
                    userId,
                    "DETAIL"
            );
        }
        return Result.success(product);
    }

    /**
     * 分页查询商品
     * /api/products/query?page=1&pageSize=10&keyword=&categoryId=&minPrice=&maxPrice=&sort_by=&sort_direction=asc
     *
     * @param request
     * @return
     */
    @GetMapping("/query")
    public Result<PagedResponse<Product>> query(ProductQueryRequest request) {
        return Result.success(productService.query(request));
    }

    /**
     * 获取所有商品
     *
     * @return
     */
    @GetMapping
    public Result<List<Product>> getAll() {
        return Result.success(productService.getAll());
    }

    @PostMapping
    public Result<Void> save(@RequestBody Product product) {
        productService.save(product);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody Product product) {
        productService.update(product);
        return Result.success();
    }

    /**
     * 删除商品
     * api/products/{id}
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id){
        productService.deleteById(id);
        return Result.success();
    }

    /**
     * 获取热销商品
     * @RequestParam 从查询字符串中提取参数，defaultValue表示默认值
     * @return
     */
    @GetMapping("/hot")
    public Result<List<ProductShowcaseResponse>> getHotProducts(
            @RequestParam(defaultValue = "6") Integer limit){
        return Result.success(productService.getHotProducts(limit == null ? 6: limit));
    }

}
