package com.example.ecommerce.config;



import io.swagger.v3.oas.models.Components;

import io.swagger.v3.oas.models.OpenAPI;

import io.swagger.v3.oas.models.info.Contact;

import io.swagger.v3.oas.models.info.Info;

import io.swagger.v3.oas.models.security.SecurityRequirement;

import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;



@Configuration

public class OpenApiConfig {



    @Bean

    public OpenAPI customOpenAPI() {

        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()

            .info(new Info()

                .title("电商平台 API 文档")

                .version("1.0.0")

                .description("Spring Boot + Vue 前后端分离电商项目的后端 API 接口文档")

                .contact(new Contact().name("ecommerce-team")))

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