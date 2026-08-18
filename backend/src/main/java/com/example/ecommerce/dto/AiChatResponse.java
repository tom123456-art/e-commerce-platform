package com.example.ecommerce.dto;

import lombok.Data;

@Data
public class AiChatResponse extends AiBaseResponse{
    // AI回复内容
    private String reply;
    // 会话ID
    private String sessionId;
}
