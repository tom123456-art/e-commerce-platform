package com.example.ecommerce.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * AI响应基类
 * 会提供三个元数据字段，所有的AI响应DTO都会继承这个类
 * 这三个字段可以让前端知道是真实AI回复还是模板降级回复
 */
public class AiBaseResponse {
    // true：模板，false：真实AI
    @Getter
    @Setter
    private boolean fallback;
    // AI服务提供者
    @Getter
    @Setter
    private String provider;
    // 使用的模型名称
    @Getter
    @Setter
    private String model;
}
