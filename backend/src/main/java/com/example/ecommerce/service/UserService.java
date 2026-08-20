package com.example.ecommerce.service;

import com.example.ecommerce.entity.User;

import java.util.List;

/**
 * 用户服务接口。
 * <p>
 * 职责：
 * - getUserById：根据 ID 查询用户
 * - getUserByUsername：根据用户名查询用户
 * - updateUser：更新用户信息
 */
public interface UserService {

    /**
     * 根据 ID 查询用户
     */
    User getUserById(Long userId);

    /**
     * 根据用户名查询用户
     */
    User getUserByUsername(String username);

    /**
     * 更新用户信息
     */
    void updateUser(User user);

    /**
     * 更新用户密码（接收已加密的密码）
     */
    void updatePassword(Long userId, String encodedPassword);

    void save(User user);

    void delete(Long id);

    List<User> getAll();

    /**
     * 对密码进行编码
     */
    String encodeIfNecessary(String password);
}
