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

            long count = redisUtil.increment(redisKey);

            if (count == 1) {

                redisUtil.expire(redisKey, WINDOW_SECONDS);

            }



            if (count > MAX_REQUESTS_PER_MINUTE) {

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

            // Redis 异常时放行

        }



        chain.doFilter(request, response);

    }



    private String getClientIp(HttpServletRequest request) {

        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {

            ip = request.getHeader("X-Real-IP");

        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {

            ip = request.getRemoteAddr();

        }

        if (ip != null && ip.contains(",")) {

            ip = ip.split(",")[0].trim();

        }

        return ip;

    }

}