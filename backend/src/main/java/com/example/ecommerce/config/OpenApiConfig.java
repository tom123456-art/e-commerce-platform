package com.example.ecommerce.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI 文档配置类。
 *
 * 启动后访问：
 *   - Swagger UI：http://localhost:8080/swagger-ui.html
 *   - OpenAPI JSON：http://localhost:8080/v3/api-docs
 *
 * 本类配置了：
 *   1. API 元信息（标题、版本、描述、联系方式）
 *   2. 全局 Bearer Token 认证方案（Swagger UI 中点击"Authorize"按钮输入 Token 即可测试需认证的接口）
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        // 定义 Bearer Token 安全方案
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
            .info(new Info()
                .title("电商平台 API 文档")
                .version("1.0.0")
                .description("Spring Boot + Vue 前后端分离电商项目的后端 API 接口文档")
                .contact(new Contact().name("ecommerce-team")))
            // 全局安全要求：所有接口默认需要 Bearer Token
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .description("输入格式：ECM.xxx.yyy（不需要加 Bearer 前缀）")));
    }
}
