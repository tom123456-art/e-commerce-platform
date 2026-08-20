package com.example.ecommerce.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * API 限流过滤器注册配置。
 * <p>
 * 限流目的：防止接口被恶意刷取或滥用（如暴力破解登录、爬虫抓取商品）。
 * <p>
 * 实现原理（固定窗口计数器）：
 * 1. 每个请求到达时，以 "rate_limit:{IP}:{时间窗口}" 为 Key 在 Redis 中计数
 * 2. 计数超过阈值（如 100 次/分钟）时返回 429 Too Many Requests
 * 3. 时间窗口过期后 Key 自动删除，计数重置
 * <p>
 * 为什么用 FilterRegistrationBean 而不是 @Component？
 * - FilterRegistrationBean 可以精确控制过滤器的 URL 匹配模式和执行顺序
 * - @Component 注册的 Filter 默认匹配所有 URL，且顺序不可控
 */
@Configuration
public class RateLimitConfig {

    /**
     * 注册限流过滤器。
     * <p>
     * setOrder(HIGHEST_PRECEDENCE)：设为最高优先级，
     * 确保限流在认证、授权等过滤器之前执行。
     * 被限流的请求直接返回 429，不再进入后续处理，节省服务器资源。
     */
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        // 只拦截 /api/ 开头的接口（静态资源和 Swagger 不限流）
        registration.addUrlPatterns("/api/*");
        // 最高优先级：限流判断在所有其他过滤器之前
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
