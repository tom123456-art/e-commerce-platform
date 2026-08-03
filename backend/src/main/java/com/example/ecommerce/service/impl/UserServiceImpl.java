package com.example.ecommerce.service.impl;

import com.example.ecommerce.entity.User;
import com.example.ecommerce.mapper.UserMapper;
import com.example.ecommerce.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务实现类。
 *
 * 职责：
 * - getUserById：根据 ID 查询用户
 * - getUserByUsername：根据用户名查询用户
 * - updateUser：更新用户信息
 *
 * 被引用：
 * - AuthController：登录后获取用户信息
 * - SecurityConfig：Spring Security 验证用户名密码时调用
 * - UserController：用户个人中心更新资料
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 根据 ID 查询用户。
     *
     * 场景：
     * - 登录后根据 Token 中的 userId 获取完整用户信息
     * - 订单详情中展示买家信息
     */
    @Override
    public User getUserById(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        if (user != null) {
            log.debug("User found by id: {}", userId);
        } else {
            log.debug("User not found by id: {}", userId);
        }
        return user;
    }

    /**
     * 根据用户名查询用户。
     *
     * 场景：
     * - 登录时根据用户名查询用户（AuthService 调用）
     * - Spring Security 的 UserDetailsService 实现中使用
     *
     * 注意：用户名有唯一约束，最多返回一条记录
     */
    @Override
    public User getUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        User user = userMapper.selectByUsername(username);
        if (user != null) {
            log.debug("User found by username: {}", username);
        } else {
            log.debug("User not found by username: {}", username);
        }
        return user;
    }

    /**
     * 更新用户信息。
     *
     * 可更新字段：邮箱、手机号、头像等（不在这里修改密码）
     *
     * 事务：虽然是单表更新，但用 @Transactional 是好习惯，
     *       以后如果需要同时更新其他表（如用户统计表），可以保证一致性
     */
    @Override
    @Transactional
    public boolean updateUser(User user) {
        if (user == null || user.getId() == null) {
            log.warn("Update user failed: user or user.id is null");
            return false;
        }

        int rows = userMapper.update(user);
        if (rows > 0) {
            log.info("User updated successfully: {}", user.getId());
            return true;
        } else {
            log.warn("Update user failed: user not found, id={}", user.getId());
            return false;
        }
    }
}
