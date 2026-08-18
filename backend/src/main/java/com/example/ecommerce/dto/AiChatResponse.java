package com.example.ecommerce.dto;

import lombok.Data;

@Data
public class AiChatResponse {
    // AI回复内容
    private String reply;
    // 会话ID
    private String sessionId;
}
