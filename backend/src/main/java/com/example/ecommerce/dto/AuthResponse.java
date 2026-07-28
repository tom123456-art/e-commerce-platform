package com.example.ecommerce.dto;

import com.example.ecommerce.entity.User;

/**
 * 登录/注册成功后的认证响应 DTO。
 *
 * 返回两个关键信息：
 *   - token：认证令牌，前端存储后用于后续请求的身份验证
 *   - user：用户基本信息，前端用于展示用户头像、昵称等
 *
 * 提供静态工厂方法 of()，简化对象创建：
 *   AuthResponse response = AuthResponse.of(token, user);
 * 比手动 new + setXxx 更简洁，且封装了内部细节。
 */
public class AuthResponse {

    /**
     * 认证令牌（Token）。
     * 本项目使用自定义 HMAC-SHA256 签名令牌（非 JWT）：
     *   1. 后端生成随机 token 字符串
     *   2. 用 HMAC-SHA256 签名保证不可篡改
     *   3. 将 token 存入 Redis（有过期时间）
     *   4. 返回给前端，前端存储在 localStorage
     * 后续请求通过 Authorization: Bearer <token> 请求头携带。
     */
    private String token;

    /** 当前登录的用户信息（注意：User Entity 中 password 字段已用 @JsonProperty(WRITE_ONLY) 排除返回） */
    private User user;

    /** 静态工厂方法：快速创建 AuthResponse 实例 */
    public static AuthResponse of(String token, User user) {
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUser(user);
        return response;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}