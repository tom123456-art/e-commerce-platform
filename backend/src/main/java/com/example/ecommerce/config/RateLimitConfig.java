package com.example.ecommerce.config;



import org.springframework.boot.web.servlet.FilterRegistrationBean;

import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;

import org.springframework.core.Ordered;



@Configuration

public class RateLimitConfig {



    @Bean

    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {

        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();

        registration.setFilter(filter);

        registration.addUrlPatterns("/api/*");

        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);

        return registration;

    }

}

