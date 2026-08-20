package com.example.ecommerce.service;

import com.example.ecommerce.dto.AuthResponse;
import com.example.ecommerce.dto.RegisterRequest;
import com.example.ecommerce.entity.User;

/**
 * 认证服务接口。
 * <p>
 * 职责：
 * - login：验证用户名密码，生成 Token
 * - register：注册新用户
 * - logout：撤销 Token
 * <p>
 * ⚠️ 本接口及其实现类 AuthServiceImpl 属于第十一章 Security 模块，
 * 因为实现类依赖 TokenService、CustomUserDetails、BCryptPasswordEncoder 等 Security 组件。
 * 第九章 Service 层和第十章 Controller 层不涉及认证逻辑。
 */
public interface AuthService {

    /**
     * 登录：验证密码，生成 Token 返回
     */
    AuthResponse login(String username, String password);

    /**
     * 注册：创建用户，生成 Token 返回
     */
    User register(RegisterRequest request);

    /**
     * 登出：撤销 Token
     */
    void logout(String token);
}
