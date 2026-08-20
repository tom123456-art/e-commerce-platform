package com.example.ecommerce.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 功能配置类
 *
 * <h2>作用</h2>
 * 统一管理所有 AI 相关功能的配置项，通过 application.yml 集中配置。
 *
 * <h2>配置绑定原理</h2>
 *
 * @ConfigurationProperties(prefix = "ecommerce.ai") 将配置文件中的属性自动映射到 Java 字段：
 * <p>
 * application.yml:
 * ecommerce:
 * ai:
 * assistant-name: "AI 助教"
 * course-name: "SpringBoot + Vue 电商平台"
 * template-fallback: true
 * <p>
 * ↓ 自动映射 ↓（kebab-case → camelCase）
 * <p>
 * AiProperties:
 * courseName = "SpringBoot + Vue 电商平台"
 * templateFallback = true
 */
@Data // [Lombok] 自动生成 getter/setter/toString/equals/hashCode
// templateFallback 字段会生成 isTemplateFallback() 和 setTemplateFallback()
@Component // [Spring] 注册为 Bean，可被注入到其他组件
@ConfigurationProperties(prefix = "ecommerce.ai")
// [Spring Boot] 绑定 ecommerce.ai.* 前缀的属性到此 Bean
// 比 @Value 更强大，支持类型安全、relaxed binding、JSR-303 校验
public class AiProperties {

    /** AI 助教名称，用于页面展示和提示词身份标识 */
//    private String assistantName = "AI 教学助教";

    /**
     * 教学案例名称，注入到 System Prompt 中让 AI 了解背景
     */
    private String courseName = "SpringBoot + Vue 前后端分离电商平台";

    /**
     * 模板降级开关（核心配置）
     * - true（默认）：AI 不可用时降级为模板回答，功能可用
     * - false：AI 不可用时抛 BusinessException，前端收到错误提示
     */
    private boolean templateFallback = true;
}
