package com.example.ecommerce.security;

import com.example.ecommerce.common.BusinessException;
import com.example.ecommerce.common.Result;
import org.springframework.security.core.Authentication;

/**
 * 安全工具类，提供获取当前登录用户的便捷方法。
 *
 * 📋 复制粘贴文件：从 02-code/11-Security/ 复制到项目中 security/ 目录。
 */
public final class SecurityUtils {

    private SecurityUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * 从 Authentication 对象中提取当前用户信息。
     * @throws BusinessException 如果用户未认证
     */
    public static CustomUserDetails currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new BusinessException(Result.UNAUTHORIZED_CODE, "Please login first");
        }
        return (CustomUserDetails) authentication.getPrincipal();
    }
}
