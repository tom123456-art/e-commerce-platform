package com.example.ecommerce.controller;

import com.example.ecommerce.common.Result;
import com.example.ecommerce.dto.AddressRequest;
import com.example.ecommerce.entity.UserAddress;
import com.example.ecommerce.security.CustomUserDetails;
import com.example.ecommerce.security.SecurityUtils;
import com.example.ecommerce.service.UserAddressService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "收货地址接口", description = "收货地址查询与管理")
@RestController
@RequestMapping("/api/addresses")
public class UserAddressController {
    private final UserAddressService userAddressService;

    public UserAddressController(UserAddressService userAddressService) {
        this.userAddressService = userAddressService;
    }

    @GetMapping
    public Result<List<UserAddress>> getAddresses(Authentication authentication) {
        CustomUserDetails user = SecurityUtils.currentUser(authentication);
        return Result.success(userAddressService.getByUserId(user.getId()));
    }

    // 新增地址
    @PostMapping
    public Result<UserAddress> create(@Valid @RequestBody AddressRequest request,
                                      Authentication authentication) {
        CustomUserDetails user = SecurityUtils.currentUser(authentication);
        return Result.success(userAddressService.create(user.getId(), request));
    }

    // 修改
    @PostMapping("/{id}")
    public Result<UserAddress> update(@PathVariable Long id,
                                      @Valid @RequestBody AddressRequest request,
                                      Authentication authentication) {
        CustomUserDetails user = SecurityUtils.currentUser(authentication);
        return Result.success(userAddressService.update(user.getId(), id, request));
    }

    // 删除
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               Authentication authentication) {
        CustomUserDetails user = SecurityUtils.currentUser(authentication);
        userAddressService.delete(user.getId(), id);
        return Result.success();
    }

    // 设置默认
    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id,
                                   Authentication authentication) {
        CustomUserDetails user = SecurityUtils.currentUser(authentication);
        userAddressService.setDefault(user.getId(), id);
        return Result.success();
    }

}
