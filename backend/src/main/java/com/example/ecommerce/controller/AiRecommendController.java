package com.example.ecommerce.controller;

import com.example.ecommerce.common.Result;
import com.example.ecommerce.dto.AiRecommendRequest;
import com.example.ecommerce.dto.AiRecommendResponse;
import com.example.ecommerce.service.AiRecommendService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 推荐接口", description = "AI 个性化商品推荐")
@RestController
@RequestMapping("/api/ai")
public class AiRecommendController {

    private final AiRecommendService aiRecommendService;

    public AiRecommendController(AiRecommendService aiRecommendService) {
        this.aiRecommendService = aiRecommendService;
    }

    @PostMapping("/recommend")
    public Result<AiRecommendResponse> recommend(@Valid @RequestBody AiRecommendRequest request) {
        return Result.success(aiRecommendService.recommend(request));
    }
}
