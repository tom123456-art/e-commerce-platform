package com.example.ecommerce.service;

import com.example.ecommerce.dto.AiChatRequest;
import com.example.ecommerce.dto.AiChatResponse;

/**
 * AI对话服务接口
 */
public interface AiChatService {
    AiChatResponse chat(AiChatRequest request);
}
