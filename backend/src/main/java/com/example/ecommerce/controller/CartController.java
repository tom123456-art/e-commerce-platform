package com.example.ecommerce.controller;

import com.example.ecommerce.common.Result;
import com.example.ecommerce.dto.CartCheckoutRequest;
import com.example.ecommerce.dto.CartItemRequest;
import com.example.ecommerce.dto.CartItemResponse;
import com.example.ecommerce.dto.OrderDetailResponse;
import com.example.ecommerce.security.CustomUserDetails;
import com.example.ecommerce.security.SecurityUtils;
import com.example.ecommerce.service.CartService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "购物车接口", description = "购物车管理")
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * 获取购物车列表
     *
     * @param authentication
     * @return
     */
    @GetMapping
    public Result<List<CartItemResponse>> getCart(Authentication authentication) {
        // 获取当前登录用户的购物车
        CustomUserDetails user = SecurityUtils.currentUser(authentication);
        return Result.success(cartService.getCart(user.getId()));
    }

    /**
     * 加入购物车
     *
     * @param request
     * @param authentication
     * @return
     */
    @PostMapping("/items")
    public Result<Void> addItem(@RequestBody CartItemRequest request,
                                Authentication authentication) {
        CustomUserDetails user = SecurityUtils.currentUser(authentication);
        cartService.addItem(user.getId(), request);
        return Result.success();
    }

    @PutMapping("/items/{id}")
    public Result<Void> updateItem(@PathVariable Long id,
                                   @RequestBody CartItemRequest request,
                                   Authentication authentication) {
        CustomUserDetails user = SecurityUtils.currentUser(authentication);
        cartService.updateItem(user.getId(), id, request.getQuantity());
        return Result.success();
    }

    @DeleteMapping("/items/{id}")
    public Result<Void> removeItem(@PathVariable Long id, Authentication authentication) {
        CustomUserDetails user = SecurityUtils.currentUser(authentication);
        cartService.removeItem(user.getId(), id);
        return Result.success();
    }

    @DeleteMapping
    public Result<Void> clear(Authentication authentication) {
        CustomUserDetails user = SecurityUtils.currentUser(authentication);
        cartService.clear(user.getId());
        return Result.success();
    }

    @PostMapping("/checkout")
    public Result<OrderDetailResponse> checkout(@Valid @RequestBody CartCheckoutRequest request,
                                                Authentication authentication) {
        CustomUserDetails user = SecurityUtils.currentUser(authentication);
        return Result.success(cartService.checkout(user.getId(), request));
    }
}
