package com.example.ecommerce.controller;


import com.example.ecommerce.common.Result;
import com.example.ecommerce.dto.CartItemRequest;
import com.example.ecommerce.dto.CartItemResponse;
import com.example.ecommerce.security.CustomUserDetails;
import com.example.ecommerce.security.SecurityUtils;
import com.example.ecommerce.service.CartService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "购物车接口", description = "购物车")
@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    //获取购物车列表
    @GetMapping
    public Result<List<CartItemResponse>> getCart(Authentication  authentication){
        CustomUserDetails user = SecurityUtils.currentUser(authentication);
        return Result.success(cartService.getCart(user.getId()));
    }

    //加入购物车
    @PostMapping("/items")
    public Result<Void> addItem(@RequestBody CartItemRequest request, Authentication authentication){
        CustomUserDetails user = SecurityUtils.currentUser(authentication);
        cartService.addItem(user.getId(), request);
        return Result.success();
    }
}
