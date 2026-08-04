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



@Configuration

@EnableWebSecurity

@EnableMethodSecurity

public class SecurityConfig {



    private final TokenAuthenticationFilter tokenAuthenticationFilter;

    private final CustomUserDetailsService userDetailsService;



    public SecurityConfig(TokenAuthenticationFilter tokenAuthenticationFilter,

                          CustomUserDetailsService userDetailsService) {

        this.tokenAuthenticationFilter = tokenAuthenticationFilter;

        this.userDetailsService = userDetailsService;

    }



    @Bean

    public PasswordEncoder passwordEncoder() {

        return new LegacyCompatiblePasswordEncoder();

    }



    @Bean

    public DaoAuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();

        provider.setUserDetailsService(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder());

        return provider;

    }



    @Bean

    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {

        return config.getAuthenticationManager();

    }



    @Bean

    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http

            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            .csrf(csrf -> csrf.disable())

            .sessionManagement(session ->

                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

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

            .authorizeHttpRequests(auth -> auth

                .requestMatchers(HttpMethod.OPTIONS, "/").permitAll()

                .requestMatchers("/api/auth/").permitAll()

                .requestMatchers("/swagger-ui/", "/v3/api-docs/", "/swagger-ui.html").permitAll()

                .requestMatchers(HttpMethod.GET, "/api/products/").permitAll()

                .requestMatchers("/api/users/", "/api/excel/", "/api/admin/").hasRole("ADMIN")

                .requestMatchers(HttpMethod.POST, "/api/products/").hasRole("ADMIN")

                .requestMatchers(HttpMethod.PUT, "/api/products/").hasRole("ADMIN")

                .requestMatchers(HttpMethod.DELETE, "/api/products/").hasRole("ADMIN")

                .requestMatchers("/api/merchant/").hasRole("MERCHANT")

                .anyRequest().authenticated())

            .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);



        return http.build();

    }



    @Bean

    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(List.of("http://localhost:", "http://127.0.0.1:"));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        config.setAllowedHeaders(List.of(""));

        config.setExposedHeaders(List.of("Authorization"));

        config.setAllowCredentials(true);



        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/*", config);

        return source;

    }

}

