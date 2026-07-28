package com.example.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户登录请求 DTO。
 *
 * 遵循"最小化原则"：登录只需要用户名和密码两个字段。
 * 如果用 User Entity 接收请求，前端必须传入 id、role、status 等所有字段，
 * 既不合理也不安全（攻击者可能注入 role="admin" 篡改权限）。
 *
 * 登录时不校验密码强度（长度、复杂度），因为用户输入的是已有密码。
 * 格式校验在注册时由 RegisterRequest 负责。
 */
public class AuthLoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}