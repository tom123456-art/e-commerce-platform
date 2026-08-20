package com.example.ecommerce.controller;

import com.example.ecommerce.common.BusinessException;
import com.example.ecommerce.common.Result;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.Review;
import com.example.ecommerce.mapper.ProductMapper;
import com.example.ecommerce.mapper.ReviewMapper;
import com.example.ecommerce.security.CustomUserDetails;
import com.example.ecommerce.security.TokenService;
import com.example.ecommerce.service.ProductService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "商家商品管理", description = "商家商品的管理")
@RestController
@RequestMapping("/api/merchant/products")
public class MerchantProductController {

    private final TokenService tokenService;
    private final ProductService productService;
    private final ProductMapper productMapper;
    private final ReviewMapper reviewMapper;

    public MerchantProductController(TokenService tokenService, ProductService productService, ProductMapper productMapper, ReviewMapper reviewMapper) {
        this.tokenService = tokenService;
        this.productService = productService;
        this.productMapper = productMapper;
        this.reviewMapper = reviewMapper;
    }

    /**
     * 获取当前商家的所有商品
     * GET /api/merchant/products
     *
     * @param token
     * @return
     */
    @GetMapping
    public Result<List<Product>> getMyProducts(@RequestHeader("Authorization") String token) {
        // 获取商家ID
        Long merchantId = getMerchantId(token);
        // 获取所有的商品
        List<Product> products = productService.getAll();
        // 筛选
        List<Product> productList = products.stream().filter(
                p -> p.getMerchantId().equals(merchantId)
        ).collect(Collectors.toList());
        return Result.success(productList);
    }

    /**
     * 获取单个商品详情
     * GET /api/merchant/products/{id}
     *
     * @param token
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result<Product> getProduct(@RequestHeader("Authorization") String token,
                                      @PathVariable("id") Long id) {
        Long merchantId = getMerchantId(token);
        // 通过商品id判断商品是否存在
        Product existing = productMapper.selectById(id);
        if (existing == null || !merchantId.equals(existing.getMerchantId())) {
            return Result.failure(Result.NOT_FOUND_CODE, "商品不存在");
        }
        return Result.success(existing);
    }

    /**
     * 创建商品
     * POST /api/merchant/products
     *
     * @param product
     * @return
     */
    @PostMapping
    public Result<Void> createProduct(@RequestHeader("Authorization") String token,
                                      @RequestBody Product product) {
        Long merchantId = getMerchantId(token);
        product.setMerchantId(merchantId);
        productService.save(product);
        return Result.success();
    }


    /**
     * 更新商品
     * PUT /api/merchant/products/{id}
     *
     * @param id
     * @param product
     * @return
     */
    @PutMapping("/{id}")
    public Result<Void> updateProduct(@RequestHeader("Authorization") String token,
                                      @PathVariable("id") Long id,
                                      @RequestBody Product product) {
        Long merchantId = getMerchantId(token);
        Product existing = productMapper.selectById(id);
        if (existing == null || !merchantId.equals(existing.getMerchantId())) {
            return Result.failure(Result.NOT_FOUND_CODE, "商品不存在");
        }
        // 强制设置ID，防止客户端篡改
        product.setId(id);
        product.setMerchantId(merchantId);
        productService.update(product);
        return Result.success();
    }

    /**
     * 删除商品
     * DELETE /api/merchant/products/{id}
     *
     * @param token
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteProduct(@RequestHeader("Authorization") String token,
                                      @PathVariable("id") Long id) {
        Long merchantId = getMerchantId(token);
        Product existing = productMapper.selectById(id);
        if (existing == null || !merchantId.equals(existing.getMerchantId())) {
            return Result.failure(Result.NOT_FOUND_CODE, "商品不存在");
        }
        productService.deleteById(id);
        return Result.success();
    }

    @GetMapping("/{id}/reviews")
    public Result<List<Review>> getProductReviews(@RequestHeader("Authorization") String token,
                                                  @PathVariable("id") Long id) {
        Long merchantId = getMerchantId(token);
        Product existing = productMapper.selectById(id);
        if (existing == null || !merchantId.equals(existing.getMerchantId())) {
            return Result.failure(Result.NOT_FOUND_CODE, "商品不存在");
        }
        return Result.success(reviewMapper.selectByProductId(id));
    }


    private Long getMerchantId(String token) {
        if (token == null || !token.startsWith("Bearer "))
            throw new BusinessException(Result.UNAUTHORIZED_CODE, "未提供有效的认证Token");
        CustomUserDetails userDetails = tokenService.parseToken(token.substring(7));
        if (userDetails == null)
            throw new BusinessException(Result.FORBIDDEN_CODE, "TOKEN无效");
        if (!userDetails.isMerchant())
            throw new BusinessException(Result.FORBIDDEN_CODE, "无权限访问");
        return userDetails.getId();
    }
}
