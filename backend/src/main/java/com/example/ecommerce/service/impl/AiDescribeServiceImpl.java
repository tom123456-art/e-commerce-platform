package com.example.ecommerce.service.impl;

import com.example.ecommerce.common.BusinessException;
import com.example.ecommerce.common.Result;
import com.example.ecommerce.config.AiProperties;
import com.example.ecommerce.dto.AiDescribeRequest;
import com.example.ecommerce.dto.AiDescribeResponse;
import com.example.ecommerce.service.AiDescribeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * AI 文案生成服务实现：商品信息 → LLM → JSON 结构化文案
 * <p>
 * 设计模式：
 * 1. 生成器模式：AiDescribeResponse 使用 @Builder
 * 2. 模板方法：generateDescription 定义"尝试 AI → 降级模板"流程骨架
 * 3. 提示词工程：要求 LLM 返回 JSON，便于程序解析
 */
@Service
public class AiDescribeServiceImpl implements AiDescribeService {

    private static final Logger log = LoggerFactory.getLogger(AiDescribeServiceImpl.class);

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();  // Jackson JSON 解析器
    private final Environment environment;
    private final AiProperties aiProperties;

    public AiDescribeServiceImpl(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
                                 Environment environment,
                                 AiProperties aiProperties) {
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        this.chatClient = builder == null ? null : builder.build();
        this.environment = environment;
        this.aiProperties = aiProperties;
    }

    @Override
    public AiDescribeResponse generateDescription(AiDescribeRequest request) {
        String productName = request.getProductName();
        String category = request.getCategory();
        String keyFeatures = request.getKeyFeatures();
        String style = request.getStyle();

        // 1. 尝试 AI 生成
        if (chatClient != null) {
            try {
                String prompt = buildPrompt(productName, category, keyFeatures, style);
                String completion = chatClient.prompt()
                        .system("你是一个专业的电商文案撰写专家，擅长撰写吸引人的商品描述、SEO标题和卖点亮点。请用简体中文回答。")
                        .user(prompt)
                        .call()
                        .content();

                if (StringUtils.hasText(completion)) {
                    return parseAiResponse(completion);
                }
            } catch (Exception ex) {
                log.warn("AI describe fallback to template mode", ex);
            }
        }

        // 2. 降级为模板生成
        if (aiProperties.isTemplateFallback()) {
            return generateTemplateResponse(productName, category, keyFeatures);
        }

        // 3. 不允许降级 → 抛异常
        throw new BusinessException(Result.ERROR_CODE, "当前未启用 Spring AI，请先配置模型后再试");
    }

    /**
     * 构建文案生成提示词
     * 关键：明确输出 JSON 格式，避免 LLM 自由发挥
     */
    private String buildPrompt(String productName, String category, String keyFeatures, String style) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请为以下商品生成电商文案：\n");
        prompt.append("商品名称：").append(productName).append("\n");

        // 可选字段：只有填了才加入 prompt，避免"null"污染
        if (StringUtils.hasText(category)) {
            prompt.append("商品分类：").append(category).append("\n");
        }
        if (StringUtils.hasText(keyFeatures)) {
            prompt.append("商品特点：").append(keyFeatures).append("\n");
        }
        if (StringUtils.hasText(style)) {
            prompt.append("文案风格：").append(style).append("\n");
        }

        // 明确输出格式（JSON schema 示例）
        prompt.append("\n请按以下JSON格式返回：\n");
        prompt.append("{\n");
        prompt.append("  \"description\": \"商品描述文案（100-200字）\",\n");
        prompt.append("  \"seoTitle\": \"SEO优化标题（20-30字）\",\n");
        prompt.append("  \"highlights\": [\"卖点1\", \"卖点2\", \"卖点3\"]\n");
        prompt.append("}\n");
        prompt.append("只返回JSON，不要其他内容。");

        return prompt.toString();
    }

    /**
     * 解析 AI 返回的 JSON 响应（用 Jackson，比字符串分割更规范）
     * 失败时返回原始文本作为 description（兜底）
     */
    private AiDescribeResponse parseAiResponse(String response) {
        try {
            String json = response.trim();
            // 清理 markdown 代码块标记
            if (json.startsWith("```json")) {
                json = json.substring(7);
            }
            if (json.startsWith("```")) {
                json = json.substring(3);
            }
            if (json.endsWith("```")) {
                json = json.substring(0, json.length() - 3);
            }
            json = json.trim();

            // Jackson 解析 JSON 树
            JsonNode root = objectMapper.readTree(json);
            String description = root.has("description") ? root.get("description").asText() : "";
            String seoTitle = root.has("seoTitle") ? root.get("seoTitle").asText() : "";

            // 解析卖点数组
            List<String> highlights = new java.util.ArrayList<>();
            if (root.has("highlights") && root.get("highlights").isArray()) {
                for (JsonNode node : root.get("highlights")) {
                    highlights.add(node.asText());
                }
            }

            // @Builder 链式构造
            return AiDescribeResponse.builder()
                    .description(description)
                    .seoTitle(seoTitle)
                    .highlights(highlights)
                    .build();
        } catch (Exception e) {
            // 解析失败：把原始文本作为 description（保证有内容返回）
            log.warn("Failed to parse AI response as JSON, using raw text", e);
            return AiDescribeResponse.builder()
                    .description(response)
                    .build();
        }
    }

    /**
     * 模板化文案生成（降级方案）
     * 用 String.format 拼接基础文案，保证功能可用
     */
    private AiDescribeResponse generateTemplateResponse(String productName, String category, String keyFeatures) {
        String description = String.format(
                "%s是一款优质的%s产品。%s品质保证，值得信赖。",
                productName,
                category != null ? category : "",
                keyFeatures != null ? keyFeatures + "。" : ""
        );

        String seoTitle = String.format("【优选】%s - 品质之选", productName);

        List<String> highlights = Arrays.asList(
                "品质保证，正品行货",
                "价格实惠，性价比高",
                "售后无忧，七天退换"
        );

        return AiDescribeResponse.builder()
                .description(description)
                .seoTitle(seoTitle)
                .highlights(highlights)
                .build();
    }
}
