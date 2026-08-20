package com.example.ecommerce.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类。
 * <p>
 * 实现 WebMvcConfigurer 接口，自定义 Spring MVC 行为：
 * - addResourceHandlers：配置静态资源映射（本项目使用）
 * - addCorsMappings：配置跨域（本项目在 SecurityConfig 中配置）
 * - addInterceptors：注册拦截器
 *
 * @Value("${app.upload.dir:uploads}")： 从 application.yml 读取 app.upload.dir 配置项，
 * 如果不存在，使用默认值 "uploads"（":" 后是默认值语法）
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 上传文件的存储目录（磁盘路径），默认 "uploads"
     */
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * 上传文件的 URL 访问前缀，默认 "/uploads"
     */
    @Value("${app.upload.url-prefix:/uploads}")
    private String urlPrefix;

    /**
     * 静态资源映射：将 URL 路径映射到服务器磁盘目录。
     * <p>
     * 示例：
     * urlPrefix = "/uploads"
     * uploadDir = "uploads"
     * <p>
     * 浏览器请求 http://localhost:8080/uploads/avatar.jpg 时：
     * 1. Spring MVC 匹配到 urlPrefix + "/**" 模式
     * 2. 将 URL 中的 "/uploads/" 替换为 "file:uploads/"
     * 3. 读取磁盘文件 uploads/avatar.jpg
     * <p>
     * "file:" 前缀告诉 Spring 这是文件系统路径（而非 classpath 路径）
     * 末尾的 "/" 很重要--确保路径拼接正确
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(urlPrefix + "/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}
