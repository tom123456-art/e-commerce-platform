package com.example.ecommerce.common;

import com.example.ecommerce.security.CustomUserDetails;

/**
 * 统一权限校验器 —— 从各 Controller 中提取的通用校验逻辑。
 *
 * 【设计原则】
 *   - 私有构造方法：工具类不应被实例化
 *   - 静态方法：方便直接 PermissionChecker.checkAccess(...) 调用
 *   - 抛 BusinessException：与全局异常处理器配合，自动转为友好的错误响应
 *
 * 【与 @PreAuthorize 的区别】
 *   - @PreAuthorize("hasRole('ADMIN')")：路径级角色校验，由 Spring Security 拦截器执行
 *   - PermissionChecker：业务级资源所有权校验，由 Controller 在方法内主动调用
 *   两者配合使用：先过角色校验，再做资源所有权校验
 */
public class PermissionChecker {

    /** 私有构造：禁止实例化工具类 */
    private PermissionChecker() {}

    /**
     * 检查当前用户是否有权访问指定资源。
     *
     * 【规则】
     *   - 未登录 → 401 UNAUTHORIZED
     *   - ADMIN 角色 → 直接放行（管理员可访问任何资源）
     *   - 普通用户 → 只能访问自己的资源（resourceOwnerId 必须等于当前用户 ID）
     *
     * @param currentUser 当前登录用户详情（从 Authentication.getPrincipal() 获取）
     * @param resourceOwnerId 资源所有者 ID（如订单的 userId、地址的 userId）
     * @throws BusinessException 如果无权访问
     */
    public static void checkAccess(Object currentUser, Long resourceOwnerId) {
        if (currentUser == null) {
            throw new BusinessException(Result.UNAUTHORIZED_CODE, "用户未登录");
        }
        if (!(currentUser instanceof CustomUserDetails)) {
            throw new BusinessException(Result.UNAUTHORIZED_CODE, "无效的用户对象");
        }
        CustomUserDetails userDetails = (CustomUserDetails) currentUser;
        // ADMIN 直接放行 —— 管理员可访问任何用户的数据
        if (userDetails.isAdmin()) return;
        // 普通用户只能访问自己的资源
        if (!userDetails.getId().equals(resourceOwnerId)) {
            throw new BusinessException(Result.FORBIDDEN_CODE, "无权访问该资源");
        }
    }

    /**
     * 检查商户是否有权操作指定商品。
     *
     * 商户只能操作自己店铺的商品（通过 merchant_id 关联）。
     * 商品不存在 → 404；不是自己的商品 → 403。
     */
    public static com.example.ecommerce.entity.Product checkProductOwnership(
            com.example.ecommerce.entity.Product existing, Long merchantId, String action) {
        if (existing == null) {
            throw new BusinessException(Result.NOT_FOUND_CODE, "商品不存在");
        }
        if (!merchantId.equals(existing.getMerchantId())) {
            throw new BusinessException(Result.FORBIDDEN_CODE, "无权" + action);
        }
        return existing;
    }

    /**
     * 校验 Authorization 请求头格式与 Token 有效性。
     */
    public static void checkAuthorization(String authorizationHeader, Object userDetails) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new BusinessException(Result.UNAUTHORIZED_CODE, "Invalid authorization header");
        }
        if (userDetails == null) {
            throw new BusinessException(Result.UNAUTHORIZED_CODE, "Token无效");
        }
    }

    /**
     * 检查目录路径安全性（防目录穿越攻击）。
     * 扫描路径必须以根路径为前缀，否则拒绝访问。
     */
    public static String checkPathSafety(String scanPath, String rootPath) {
        if (scanPath == null || rootPath == null) {
            throw new BusinessException(Result.FORBIDDEN_CODE, "Access denied: invalid path");
        }
        // 统一替换为正斜杠后比较前缀，兼容 Windows 与 Linux
        String normalizedScan = scanPath.replace("\\", "/");
        String normalizedRoot = rootPath.replace("\\", "/");
        if (!normalizedScan.startsWith(normalizedRoot)) {
            throw new BusinessException(Result.FORBIDDEN_CODE, "Access denied: directory outside root");
        }
        return scanPath;
    }
}
