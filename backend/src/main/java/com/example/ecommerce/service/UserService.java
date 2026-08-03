package com.example.ecommerce.service;

import com.example.ecommerce.entity.User;

/**
 * 用户服务接口。
 *
 * 职责：
 * - getUserById：根据 ID 查询用户
 * - getUserByUsername：根据用户名查询用户
 * - updateUser：更新用户信息
 */
public interface UserService {

    /** 根据 ID 查询用户 */
    User getUserById(Long userId);

    /** 根据用户名查询用户 */
    User getUserByUsername(String username);

    /** 更新用户信息 */
    boolean updateUser(User user);
}
