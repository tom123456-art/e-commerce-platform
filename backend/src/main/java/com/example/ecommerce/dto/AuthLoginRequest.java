package com.example.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求DTO
 * 遵循最小化原则，只需要用户名和密码两个字段
 * 登录的时候不需要校验密码的强度
 */
public class AuthLoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
