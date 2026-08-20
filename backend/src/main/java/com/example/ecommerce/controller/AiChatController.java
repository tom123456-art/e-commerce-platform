package com.example.ecommerce.controller;

import com.example.ecommerce.common.Result;
import com.example.ecommerce.dto.AiChatRequest;
import com.example.ecommerce.dto.AiChatResponse;
import com.example.ecommerce.service.AiChatService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI客服接口", description = "AI智能客服对话")
@RestController
@RequestMapping("/api/ai")
public class AiChatController {


    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    /**
     * POST  /api/ai/chat
     * 用POST的原因：
     * 1、请求体中包含内容
     * 2、保存会话历史
     *
     * @param request
     * @return
     */
    @PostMapping("/chat")
    public Result<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        return Result.success(aiChatService.chat(request));
    }
}
