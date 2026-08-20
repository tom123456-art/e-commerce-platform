package com.example.ecommerce.common;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * XSS 过滤器。
 *
 * @Component 让 Spring 自动注册这个过滤器。
 * 所有 HTTP 请求在到达 Controller 之前，都会经过这个过滤器。
 * 过滤器会自动清理请求参数中的 XSS 攻击内容。
 */
@Component
public class XssFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // 用包装类替换原始请求，自动清理参数
        chain.doFilter(new XssRequestWrapper((HttpServletRequest) request), response);
    }

    /**
     * 请求包装类：重写 getParameter 方法，自动清理 XSS 内容。
     */
    private static class XssRequestWrapper extends HttpServletRequestWrapper {

        public XssRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return XssUtils.clean(value); // 自动清理
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) return null;
            String[] cleaned = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                cleaned[i] = XssUtils.clean(values[i]);
            }
            return cleaned;
        }
    }
}