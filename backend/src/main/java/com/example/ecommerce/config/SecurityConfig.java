package com.example.ecommerce.config;

import com.example.ecommerce.security.CustomUserDetailsService;
import com.example.ecommerce.security.LegacyCompatiblePasswordEncoder;
import com.example.ecommerce.security.TokenAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 核心配置类。
 *
 * @EnableWebSecurity：启用 Spring Security 的 Web 安全功能
 * @EnableMethodSecurity：启用方法级权限控制（@PreAuthorize("hasRole('ADMIN')")）
 *
 * 本类定义了：
 *   1. 密码编码器（BCrypt + 历史兼容）
 *   2. 认证管理器（用户登录验证）
 *   3. 安全过滤器链（URL 权限规则 + CORS + Token 过滤器）
 *
 * ⚠️ 本类不使用构造器注入，所有 @Bean 依赖通过方法参数注入，
 *    以避免与 TokenAuthenticationFilter 形成循环依赖：
 *    SecurityConfig -> TokenAuthenticationFilter -> TokenService -> UserServiceImpl -> PasswordEncoder(@Bean in SecurityConfig) -> 循环
 *
 * 完整的认证流程讲解见 03-用户认证模块。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * 密码编码器。
     * 使用自定义的 LegacyCompatiblePasswordEncoder：
     *   - 新密码用 BCrypt 加密存储
     *   - 兼容历史遗留的明文密码（登录时自动升级为 BCrypt）
     *
     * ⚠️ 此 Bean 独立于构造器注入，因为 UserServiceImpl 依赖 PasswordEncoder，
     *    而 TokenService 依赖 UserService、TokenAuthenticationFilter 依赖 TokenService，
     *    若 PasswordEncoder 放在需要构造器注入的类中会形成循环依赖。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new LegacyCompatiblePasswordEncoder();
    }

    /**
     * 认证提供者：将 UserDetailsService + PasswordEncoder 组装在一起。
     * 登录时 Spring Security 调用此提供者验证用户名和密码。
     *
     * 依赖通过方法参数注入（而非构造器），避免与 TokenAuthenticationFilter 形成循环依赖。
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(CustomUserDetailsService userDetailsService,
                                                            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /** 认证管理器：供 AuthService 在登录时调用 authenticate() 方法 */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * 安全过滤器链 -- 定义 URL 级别的访问控制规则。
     *
     * 规则匹配顺序：从上到下，第一个匹配的规则生效。
     *
     * 设计原则：
     *   - 公开接口（permitAll）：注册、登录、Swagger、商品浏览
     *   - 管理员接口（hasRole("ADMIN")）：用户管理、商品增删改、Excel 导入导出
     *   - 商家接口（hasRole("MERCHANT")）：商家中心
     *   - 认证接口（authenticated）：购物车、订单、支付、地址
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    TokenAuthenticationFilter tokenAuthenticationFilter) throws Exception {
        http
            // 启用 CORS（跨域），使用下方定义的 corsConfigurationSource
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 禁用 CSRF：本项目使用无状态 Token 认证，不依赖 Cookie，无需 CSRF 防护
            .csrf(csrf -> csrf.disable())
            // 会话策略：STATELESS 表示不创建 HttpSession，每次请求独立认证
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 自定义 401/403 响应格式（返回 JSON 而非默认 HTML 错误页）
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(401);
                    response.getWriter().write(
                        "{\"success\":false,\"code\":401,\"message\":\"Please login first\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(403);
                    response.getWriter().write(
                        "{\"success\":false,\"code\":403,\"message\":\"No permission\"}");
                }))
            // URL 权限规则
            .authorizeHttpRequests(auth -> auth
                // 公开接口：OPTIONS 预检请求、注册登录、Swagger 文档
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                // 公开接口：商品浏览（GET 请求）
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                // 管理员接口：用户管理、Excel 导入导出、后台管理
                .requestMatchers("/api/users/**", "/api/excel/**", "/api/admin/**").hasRole("ADMIN")
                // 管理员接口：商品的增删改（GET 已公开）
                .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
                // 商家接口
                .requestMatchers("/api/merchant/**").hasRole("MERCHANT")
                // 其他所有接口需要认证
                .anyRequest().authenticated())
            // 将 Token 过滤器插入到 UsernamePasswordAuthenticationFilter 之前
            // 这样每个请求先经过 Token 解析，再进入 Spring Security 的认证流程
            .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS 跨域配置。
     * Cross-Origin Resource Sharing
     * 前后端分离架构中，前端（localhost:3000）和后端（localhost:8080）端口不同，
     * 浏览器的同源策略会阻止跨域请求，需要后端显式允许。
     *
     * 生产环境应将 allowedOrigins 改为实际的前端域名。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许的前端源（开发环境）
        config.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        // 允许的 HTTP 方法
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // 允许的请求头
        config.setAllowedHeaders(List.of("*"));
        // 暴露给前端的响应头（前端需要读取 Authorization）
        config.setExposedHeaders(List.of("Authorization"));
        // 允许携带凭证（Cookie、Authorization 头）
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
