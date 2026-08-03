package com.example.ecommerce.dto;

import com.example.ecommerce.entity.User;

/**
 * 注册、登录成功后的认证响应DTO
 * 返回两个关键信息：
 * token:认证令牌，前端存储用来后续请求的身份验证
 * user:用户的基本信息，前端可以拿来展示头像、昵称等
 */
public class AuthResponse {

    private String token;
    private User user;

    /**
     * 静态工厂方法，用来快速创建AuthResponse实例
     */
    public static AuthResponse of(String token, User user){
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUser(user);
        return response;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
