package com.example.ecommerce.controller;

import com.example.ecommerce.common.Result;
import com.example.ecommerce.dto.AiSearchRequest;
import com.example.ecommerce.dto.AiSearchResponse;
import com.example.ecommerce.service.AiSearchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 搜索接口", description = "AI 驱动的商品搜索")
@RestController
@RequestMapping("/api/ai")
public class AiSearchController {

    private final AiSearchService aiSearchService;

    public AiSearchController(AiSearchService aiSearchService) {
        this.aiSearchService = aiSearchService;
    }

    /**
     * POST /api/ai/search
     * 用 POST：1) 自然语言查询可能很长，GET 有 URL 长度限制；
     *         2) 请求体含复杂 JSON 结构；3) 避免搜索词记录在浏览器历史
     */
    @PostMapping("/search")
    public Result<AiSearchResponse> search(@Valid @RequestBody AiSearchRequest request) {
        return Result.success(aiSearchService.search(request));
    }
}
