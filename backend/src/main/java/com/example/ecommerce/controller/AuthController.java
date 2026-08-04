package com.example.ecommerce.controller;

import com.example.ecommerce.common.Result;
import com.example.ecommerce.dto.AuthLoginRequest;
import com.example.ecommerce.dto.AuthResponse;
import com.example.ecommerce.dto.RegisterRequest;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.security.CustomUserDetails;
import com.example.ecommerce.security.TokenService;
import com.example.ecommerce.service.AuthService;
import com.example.ecommerce.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器 —— 处理登录、注册、退出。
 *
 * 所有接口路径以 /api/auth 为前缀，由 SecurityConfig 配置为公开接口（无需认证）。
 * 响应统一用 Result<T> 包装，符合项目 API 规范。
 *
 * @Tag：Swagger/OpenAPI 文档分组注解，在接口文档中归类显示
 */
@Tag(name = "认证接口", description = "用户登录、注册、登出")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;
    private final UserService userService;

    // 构造函数注入（Spring 推荐，保证依赖不可变、便于测试）
    public AuthController(AuthService authService, TokenService tokenService, UserService userService) {
        this.authService = authService;
        this.tokenService = tokenService;
        this.userService = userService;
    }

    /**
     * 登录接口 —— 公开接口（无需认证）。
     *
     * @Valid：触发 AuthLoginRequest 上的 @NotBlank 校验，失败时由 GlobalExceptionHandler 返回 400
     * @RequestBody：将请求体 JSON 反序列化为 AuthLoginRequest 对象
     */
    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody AuthLoginRequest request) {
        return Result.success(authService.login(request.getUsername(), request.getPassword()));
    }

    /**
     * 获取当前登录用户信息 —— 用于前端刷新页面后恢复会话。
     *
     * 不使用 @AuthenticationPrincipal 是为了显式控制错误响应：
     * - 未携带 Token → 401 "未提供有效的认证Token"
     * - Token 无效/过期 → 401 "Token无效或已过期"
     * - 用户已被删除 → 404 "用户不存在"
     *
     * @RequestHeader(required = false)：允许未携带 Authorization 头（不强制）
     */
    @GetMapping("/me")
    public Result<User> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        // 校验请求头格式
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Result.failure(401, "未提供有效的认证Token");
        }
        // 截取 "Bearer " 之后的部分（7 个字符），调用 TokenService 验证
        CustomUserDetails userDetails = tokenService.parseToken(authHeader.substring(7));
        if (userDetails == null) return Result.failure(401, "Token无效或已过期");

        // 通过 id 查询完整用户信息（password 字段会被 @JsonProperty(WRITE_ONLY) 自动剔除）
        User user = userService.getUserById(userDetails.getId());
        if (user == null) return Result.failure(404, "用户不存在");
        // 双重防护：虽然 @JsonProperty(WRITE_ONLY) 已在序列化时排除密码，这里显式置空防止意外泄露
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 注册接口 —— 公开接口（无需认证）。
     * RegisterRequest 上有完整的字段校验注解（用户名/密码强度/邮箱/手机号）。
     */
    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    /**
     * 退出接口 —— 需认证（但实际上即使未携带 Token 也返回成功，避免前端处理复杂错误）。
     *
     * 登出的核心动作是调用 TokenService.revokeToken() 删除 Redis 中的会话记录，
     * 使该 Token 在下一次 parseToken 时失效（即使签名仍然正确）。
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authService.logout(authHeader.substring(7));
        }
        return Result.success();
    }
}
