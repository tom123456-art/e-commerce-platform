package com.example.ecommerce.service.impl;

import com.example.ecommerce.common.BusinessException;
import com.example.ecommerce.common.Result;
import com.example.ecommerce.dto.AuthResponse;
import com.example.ecommerce.dto.RegisterRequest;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.security.CustomUserDetails;
import com.example.ecommerce.security.TokenService;
import com.example.ecommerce.service.AuthService;
import com.example.ecommerce.service.UserService;
import com.example.ecommerce.utils.SensitiveDataUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现类。
 * <p>
 * 职责：
 * - login：验证用户名密码，生成 Token
 * - register：注册新用户，生成 Token
 * - logout：撤销 Token
 * <p>
 * 依赖：
 * - UserMapper：查询/保存用户
 * - TokenService（security 包）：生成/验证/撤销 Token，Token 格式为 ECM.{rawToken}.{hmacSignature}
 * - BCryptPasswordEncoder：密码加密与验证
 * <p>
 * ⚠️ 本类属于第十一章 Security 模块，因为依赖 TokenService、CustomUserDetails、BCryptPasswordEncoder。
 * 第九章 Service 层和第十章 Controller 层不涉及认证逻辑，认证相关代码统一在第十一章引入。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    /**
     * 三个核心组件
     * - UserService：用户信息查询/保存
     * - TokenService：Token 生成/验证/撤销
     * - AuthenticationManager：Spring Security 提供的认证入口
     */
    private final UserService userService;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;

    public AuthServiceImpl(UserService userService,
                           TokenService tokenService,
                           AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * 用户登录：验证密码，生成 Token 返回。
     * <p>
     * 流程：
     * 1. 根据用户名查询用户（用户名唯一）
     * 2. 校验用户是否存在
     * 3. 校验密码是否正确
     * 4. 检查用户状态（是否被禁用）
     * 5. 生成 Token（通过 CustomUserDetails.fromUser 转换后调用 TokenService.createToken）
     * 6. 返回 Token + 用户信息
     */
    @Override
    public AuthResponse login(String username, String password) {
        // 1. 调用AuthenticationManager触发认证流程
        // 内部委托DaoAuthenticationProvider完成
        // 首先调用CustomUserDetailsService.loadUserByUsername加载用户详情
        // 然后用BCryptPasswordEncoder验证密码
        // 检查UserDetails是否被禁用
        // 如果以上步骤都通过，则返回一个Authentication对象，如果认证失败，则抛出异常
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );
        // 2、从认证的结果中提取已认证的用户信息
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        // 3、生成Token然后存入双层缓存（本地+redis）
        String token = tokenService.createToken(userDetails);
        // 4、从数据库查询完整的用户信息
        User user = userService.getUserByUsername(username);
        log.info("用户登录成功，用户名：{}，Token：{}", username, token);
        return AuthResponse.of(token, user);
    }

    /**
     * 用户注册：创建用户，生成 Token 返回。
     * <p>
     * 流程：
     * 1. 检查用户名是否已存在
     * 2. 检查邮箱是否已注册
     * 3. 加密密码（BCrypt）
     * 4. 插入用户记录
     * 5. 生成 Token（自动登录）
     * 6. 返回 Token + 用户信息
     */
    @Override
    public User register(RegisterRequest request) {
        // 基础校验
        if (request == null)
            throw new BusinessException(Result.BAD_REQUEST_CODE, "注册信息不能为空");
        // 两次密码输入一致性校验
        if (request.getPassword() == null ||
                !request.getPassword().equals(request.getConfirmPassword()))
            throw new BusinessException(Result.BAD_REQUEST_CODE, "两次输入的密码不一致");
        // 用户名唯一校验
        if (userService.getUserByUsername(request.getUsername()) != null)
            throw new BusinessException(Result.CONFLICT_CODE, "用户名已存在");
        // 构建User实体
        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().trim());
        user.setPassword(userService.encodeIfNecessary(request.getPassword()));
        user.setNickname(request.getNickname().trim());
        user.setPhone(request.getPhone().trim());
        user.setRole("USER");
        user.setStatus(1);
        // 保存到数据库
        userService.save(user);
        log.info("用户注册成功，用户名：{}，邮箱：{}，手机号：{}",
                user.getUsername(), user.getEmail(), user.getPhone());
        // 返回完整的User对象
        return userService.getUserByUsername(user.getUsername());
    }

    /**
     * 用户登出：撤销 Token（删除 Redis 中的会话信息）。
     * <p>
     * 流程：
     * 1. 调用 TokenService.revokeToken()
     * 2. 返回成功（即使 Token 已过期也不算错误）
     */
    @Override
    public void logout(String token) {
        // 脱敏之后再记录日志
        log.info("用户退出登录，Token：{}",
                SensitiveDataUtil.maskToken(token));
        tokenService.revokeToken(token);
    }
}
