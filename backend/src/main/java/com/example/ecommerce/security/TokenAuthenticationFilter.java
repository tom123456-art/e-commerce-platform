package com.example.ecommerce.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Token 认证过滤器 —— 整个鉴权体系的"守门人"。
 *
 * 在每个 HTTP 请求到达 Controller 之前执行（位于 Spring Security 过滤链中）：
 * 1. 从 Authorization 请求头提取 Bearer Token
 * 2. 调用 TokenService 验证 Token（HMAC 签名 + Redis 会话查找）
 * 3. 验证通过后，将用户信息构建为 Authentication 对象写入 SecurityContextHolder
 * 4. 后续的授权检查（@PreAuthorize、hasRole 等）依赖 SecurityContext 中的 Authentication
 *
 * 📋 复制粘贴文件：从 02-code/11-Security/ 复制到项目中 security/ 目录。
 */
@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    public TokenAuthenticationFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            CustomUserDetails userDetails = tokenService.parseToken(token);

            if (userDetails != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
