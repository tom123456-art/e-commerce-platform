package com.example.ecommerce.controller;

import com.example.ecommerce.common.BusinessException;
import com.example.ecommerce.common.Result;
import com.example.ecommerce.dto.*;
import com.example.ecommerce.entity.Review;
import com.example.ecommerce.entity.Store;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.security.CustomUserDetails;
import com.example.ecommerce.security.TokenService;
import com.example.ecommerce.service.AuthService;
import com.example.ecommerce.service.MerchantService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "商家接口", description = "商家和商品的管理")
@RestController
@RequestMapping("/api/merchant")
public class MerchantController {

    private final AuthService authService;
    private final TokenService tokenService;
    private final MerchantService merchantService;

    public MerchantController(AuthService authService, TokenService tokenService, MerchantService merchantService) {
        this.authService = authService;
        this.tokenService = tokenService;
        this.merchantService = merchantService;
    }

    /**
     * 商家注册
     * POST /api/merchant/register
     */
    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody MerchantRegisterRequest request){
        return Result.success(merchantService.register(request));
    }

    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody AuthLoginRequest request){
        return Result.success(authService.login(request.getUsername(), request.getPassword()));
    }

    // 需要认证才可以登录

    /**
     * 获取当前商家信息
     * GET /api/merchant/me
     * @param token
     * @return
     */
    @GetMapping("/me")
    public Result<User> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String token){
        if (token == null || !token.startsWith("Bearer ")){
            return Result.failure(401, "未提供有效的认证Token");
        }
        // 截取 "Bearer " 之后的部分（7 个字符），调用 TokenService 验证
        CustomUserDetails userDetails = tokenService.parseToken(token.substring(7));
        if (userDetails == null) return Result.failure(401, "Token无效或已过期");
        User user = new User();
        user.setId(userDetails.getId());
        user.setUsername(userDetails.getUsername());
        // 判断角色权限
        user.setRole(userDetails.isAdmin() ? "ADMIN" : (userDetails.isMerchant() ? "MERCHANT" : "USER"));
        return Result.success(user);
    }

    /**
     * 获取当前商家的店铺信息
     * GET /api/merchant/store
     * @param token
     * @return
     */
    @GetMapping("/store")
    public Result<Store> getStore(@RequestHeader("Authorization") String token){
        // 通过token获取商家的id
        Long merchantId = getMerchantId(token);
        return Result.success(merchantService.getStore(merchantId));
    }

    /**
     * 更新当前商家的店铺信息
     * PUT /api/merchant/store
     * @param token
     * @param request
     * @return
     */
    @PutMapping("/store")
    public Result<Store> updateStore(@RequestHeader("Authorization") String token,
                                     @RequestBody StoreRequest request){
        Long merchantId = getMerchantId(token);
        return Result.success(merchantService.updateStore(merchantId, request));
    }

    /**
     * 获取当前商家的店铺评价
     * GET /api/merchant/reviews
     * @param token
     * @return
     */
    @GetMapping("/reviews")
    public Result<List<Review>> getReviews(@RequestHeader("Authorization") String token){
        Long merchantId = getMerchantId(token);
        return Result.success(merchantService.getReviews(merchantId));
    }

    /**
     * 回复当前商家的店铺评价
     * POST /api/merchant/reviews/reply
     * @param token
     * @param request
     * @return
     */
    @PostMapping("/reviews/reply")
    public Result<Void> replyReviews(@RequestHeader("Authorization") String token,
                                     @Valid @RequestBody ReviewReplyRequest request){
        Long merchantId = getMerchantId(token);
        merchantService.replyReviews(merchantId, request);
        return Result.success();
    }


    /**
     * 隐藏当前商家的店铺评价
     * PUT /api/merchant/reviews/{id}/hide
     * @param token
     * @param reviewId 评论ID
     * @return
     */
    @PutMapping("/reviews/{id}/hide")
    public Result<Void> hideReviews(@RequestHeader("Authorization") String token,
                                    @PathVariable("id") Long reviewId){
        Long merchantId = getMerchantId(token);
        merchantService.hideReviews(merchantId, reviewId);
        return Result.success();
    }


    // 私有服务方法

    /**
     * 从token中获取当前商家的id，必须要去校验是否是商家
     * @param token
     * @return
     */
    private Long getMerchantId(String token){
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
