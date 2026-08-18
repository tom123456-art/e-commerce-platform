package com.example.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI对话请求DTO
 */
@Data
public class AiChatRequest {
    @NotBlank(message = "消息不能为空")
    private String message;
    // 会话id，首次对话不传，后端生成，后续对话可以携带，用于保持对话 context，可选
    private String sessionId;
}
