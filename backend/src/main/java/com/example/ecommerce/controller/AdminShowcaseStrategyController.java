package com.example.ecommerce.controller;

import com.example.ecommerce.common.Result;
import com.example.ecommerce.dto.ShowcaseStrategyRequest;
import com.example.ecommerce.dto.ShowcaseStrategyResponse;
import com.example.ecommerce.service.ShowcaseStrategyService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "展示策略管理接口", description = "商品展示策略配置与调优")
@RestController
@RequestMapping("/api/admin/showcase-strategy")
public class AdminShowcaseStrategyController {

    private final ShowcaseStrategyService showcaseStrategyService;

    public AdminShowcaseStrategyController(ShowcaseStrategyService showcaseStrategyService) {
        this.showcaseStrategyService = showcaseStrategyService;
    }

    /**
     * 获取当前策略配置
     *
     * @return
     */
    @GetMapping
    public Result<ShowcaseStrategyResponse> getStrategy() {
        return Result.success(showcaseStrategyService.getStrategy());
    }

    /**
     * 保存策略配置。
     * <p>手动模式：直接应用用户提交的权重。
     * 自动模式：保存配置后触发一次自动调优。</p>
     *
     * @param request 策略请求体（可为空，Service 用默认值填充）
     */
    @PutMapping
    public Result<ShowcaseStrategyResponse> saveStrategy(
            @RequestBody(required = false) ShowcaseStrategyRequest request) {
        return Result.success(showcaseStrategyService.saveStrategy(request));
    }

    /**
     * 触发立即自动调优 —— 基于历史数据自动优化权重。
     * <p>仅在 AUTO 模式下可触发，否则返回 400 错误。</p>
     */
    @PostMapping("/auto-tune")
    public Result<ShowcaseStrategyResponse> autoTune() {
        return Result.success(showcaseStrategyService.autoTuneNow());
    }
}
