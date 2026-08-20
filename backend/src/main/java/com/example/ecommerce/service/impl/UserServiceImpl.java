package com.example.ecommerce.service.impl;

import com.example.ecommerce.common.BusinessException;
import com.example.ecommerce.common.Result;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.mapper.UserMapper;
import com.example.ecommerce.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 用户服务实现类。
 * <p>
 * 职责：
 * - getUserById：根据 ID 查询用户
 * - getUserByUsername：根据用户名查询用户
 * - updateUser：更新用户信息
 * <p>
 * 被引用：
 * - AuthController：登录后获取用户信息
 * - SecurityConfig：Spring Security 验证用户名密码时调用
 * - UserController：用户个人中心更新资料
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    // 手机号正则
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    // 用户名正则：4-20位字母数字下划线
    private static final Pattern USERNAME_PATTERN
            = Pattern.compile("^[a-zA-Z0-9_]{4,20}$");

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 根据 ID 查询用户。
     * <p>
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
            user.setPassword(null);
            log.debug("User found by id: {}", userId);
        } else {
            log.debug("User not found by id: {}", userId);
        }
        return user;
    }

    /**
     * 根据用户名查询用户。
     * <p>
     * 场景：
     * - 登录时根据用户名查询用户（AuthService 调用）
     * - Spring Security 的 UserDetailsService 实现中使用
     * <p>
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
     * <p>
     * 可更新字段：邮箱、手机号、头像等（不在这里修改密码）
     * <p>
     * 事务：虽然是单表更新，但用 @Transactional 是好习惯，
     * 以后如果需要同时更新其他表（如用户统计表），可以保证一致性
     */
    @Override
    public void updateUser(User user) {
        if (user == null || user.getId() == null)
            throw new BusinessException(Result.BAD_REQUEST_CODE, "用户或用户ID为空");
        //  查询已有记录
        User existing = userMapper.selectById(user.getId());
        if (existing == null)
            throw new BusinessException(Result.NOT_FOUND_CODE, "用户不存在");
        // 合并，如果前端没有传值，则使用已有记录的值
        mergeMissingFields(user, existing);
        validateUserFields(user, false);
        // 密码处理
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            user.setPassword(existing.getPassword());
        } else {
            user.setPassword(encodeIfNecessary(user.getPassword()));
        }
        userMapper.update(user);
    }

    private void mergeMissingFields(User user, User existing) {
        if (user.getUsername() == null || user.getUsername().isEmpty())
            user.setUsername(existing.getUsername());
        if (user.getNickname() == null || user.getNickname().isEmpty())
            user.setNickname(existing.getNickname());
        if (user.getEmail() == null || user.getEmail().isEmpty())
            user.setEmail(existing.getEmail());
        if (user.getPhone() == null || user.getPhone().isEmpty())
            user.setPhone(existing.getPhone());
        if (user.getRole() == null)
            user.setRole(existing.getRole());
        if (user.getStatus() == null)
            user.setStatus(existing.getStatus());
    }

    @Override
    public void updatePassword(Long userId, String encodedPassword) {
        userMapper.updatePassword(userId, encodedPassword);
    }

    /**
     * @param user
     */
    @Override
    public void save(User user) {
        if (user == null || user.getUsername() == null || user.getPassword() == null)
            throw new BusinessException(Result.BAD_REQUEST_CODE, "用户名或密码不能为空");
        // 字段格式校验
        validateUserFields(user, true);
        // 设置默认值
        if (user.getStatus() == null) user.setStatus(1);
        if (user.getRole() == null) user.setRole("user");
        // 对密码进行编码加密
        user.setPassword(encodeIfNecessary(user.getPassword()));
        // 插入用户记录
        userMapper.insert(user);
    }

    /**
     * 验证用户字段是否符合要求
     *
     * @param user
     * @param b
     */
    private void validateUserFields(User user, boolean b) {
        String username = user.getUsername() == null ? "" : user.getUsername().trim();
        if (!USERNAME_PATTERN.matcher(username).matches())
            throw new BusinessException(Result.BAD_REQUEST_CODE, "用户名格式不正确");
        if (user.getPhone() != null && !user.getPhone().isEmpty() &&
                !PHONE_PATTERN.matcher(user.getPhone()).matches())
            throw new BusinessException(Result.BAD_REQUEST_CODE, "手机号格式不正确");
    }

    /**
     * @param id
     */
    @Override
    public void delete(Long id) {
        userMapper.delete(id);
    }

    /**
     * @return
     */
    @Override
    public List<User> getAll() {
        List<User> users = userMapper.selectAll();
        users.forEach(user -> {
            if (user != null) user.setPassword(null);
        });
        return users;
    }

    /**
     * 对密码进行编码
     *
     * @param password
     */
    @Override
    public String encodeIfNecessary(String password) {
        if (password == null || password.isEmpty()) return password;
        if (password.startsWith("$2a$") ||
                password.startsWith("$2b$") ||
                password.startsWith("$2y$"))
            return password;
        return passwordEncoder.encode(password);
    }
}
