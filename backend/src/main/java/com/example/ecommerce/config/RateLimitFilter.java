package com.example.ecommerce.config;

import com.example.ecommerce.utils.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * API 限流过滤器。
 *
 * 注意：本类位于 config 包，不属于 security 包。
 * 它是一个普通的 Servlet Filter（非 Spring Security Filter），
 * 不依赖 Spring Security 的任何类。
 *
 * 基于 Redis 实现滑动窗口计数器：
 * 1. 从请求中获取客户端 IP
 * 2. 以 "rate_limit:{ip}" 为 key，在 Redis 中递增计数
 * 3. 第一次访问设置 TTL（如 60 秒），后续访问计数 +1
 * 4. 超过阈值（如 100 次/分钟）返回 429 Too Many Requests
 */
@Component
public class RateLimitFilter implements Filter {

    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final int WINDOW_SECONDS = 60;
    private static final String REDIS_KEY_PREFIX = "rate_limit:";

    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RateLimitFilter(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String clientIp = getClientIp(httpRequest);
        String redisKey = REDIS_KEY_PREFIX + clientIp;

        try {
            // 原子递增
            long count = redisUtil.increment(redisKey);
            if (count == 1) {
                // 第一次访问，设置过期时间
                redisUtil.expire(redisKey, WINDOW_SECONDS);
            }

            if (count > MAX_REQUESTS_PER_MINUTE) {
                // 超限，返回 429
                httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
                httpResponse.setCharacterEncoding("UTF-8");

                Map<String, Object> body = new HashMap<>();
                body.put("code", 429);
                body.put("message", "请求过于频繁，请稍后再试");

                httpResponse.getWriter().write(objectMapper.writeValueAsString(body));
                return;
            }
        } catch (Exception e) {
            // Redis 异常时放行，不因限流故障阻断正常请求
        }

        chain.doFilter(request, response);
    }

    /**
     * 获取客户端真实 IP（支持代理转发场景）
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能包含多个 IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
