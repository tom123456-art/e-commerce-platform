package com.example.ecommerce.controller;

import com.example.ecommerce.common.Result;
import com.example.ecommerce.dto.AiDescribeRequest;
import com.example.ecommerce.dto.AiDescribeResponse;
import com.example.ecommerce.service.AiDescribeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 文案生成接口", description = "AI 商品描述与 SEO 标题生成")
@RestController
@RequestMapping("/api/ai")
public class AiDescribeController {

    private final AiDescribeService aiDescribeService;

    public AiDescribeController(AiDescribeService aiDescribeService) {
        this.aiDescribeService = aiDescribeService;
    }

    @PostMapping("/describe")
    public Result<AiDescribeResponse> describe(@Valid @RequestBody AiDescribeRequest request) {
        // 注意：Service 方法名是 generateDescription，不是 describe（历史命名）
        return Result.success(aiDescribeService.generateDescription(request));
    }
}
