package com.example.ecommerce.service.impl;

import com.example.ecommerce.common.BusinessException;
import com.example.ecommerce.common.Result;
import com.example.ecommerce.config.AiProperties;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * AI 类 Service 的公共基类：承载两个子类共用的常量、字段、构造器，
 * 以及“AI 优先、失败降级”的编排逻辑、LLM 返回 JSON 解析、商品列表拼接等能力。
 */
public abstract class AbstractAiService {

    protected static final Logger log = LoggerFactory.getLogger(AbstractAiService.class);

    protected static final String TEMPLATE_PROVIDER = "template-mock";

    protected static final Map<Integer, String> CATEGORY_LABELS =
            Collections.unmodifiableMap(Map.of(
                    1, "手机数码", 2, "电脑办公", 3, "智能家电",
                    4, "家居生活", 5, "运动户外", 6, "影音娱乐"
            ));

    private static final ObjectMapper objectMapper = new ObjectMapper();

    protected final ProductService productService;
    protected final AiProperties aiProperties;
    protected final ChatClient chatClient;
    protected final Environment env;

    protected AbstractAiService(
            ProductService productService,
            AiProperties aiProperties,
            ObjectProvider<ChatClient.Builder> chatClientProvider,
            Environment env) {
        this.productService = productService;
        this.aiProperties = aiProperties;
        // 如果 Bean 不存在的话，返回 null
        ChatClient.Builder builder = chatClientProvider.getIfAvailable();
        this.chatClient = builder == null ? null : builder.build();
        this.env = env;
    }

    /**
     * 从 Map 中取出 id（兼容 number / string）
     */
    private static Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 从 Map 中取出 score（兼容 number / string）
     */
    private static Integer toInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 统一的“AI 优先、失败降级”编排：尝试 aiCall，异常则回退到 fallbackCall。
     * 返回 provider 与 fallback 标记，由子类负责封装成各自的响应对象。
     *
     * @param warnMessage AI 调用失败时记录的日志内容（各子类语义不同）
     */
    protected <T> AiCallResult<T> executeAiCall(
            Supplier<List<T>> aiCall,
            Supplier<List<T>> fallbackCall,
            String warnMessage) {
        String provider = TEMPLATE_PROVIDER;
        boolean fallback = true;
        List<T> results;
        if (chatClient != null) {
            try {
                // 尝试 AI 调用
                results = aiCall.get();
                provider = "spring-ai";
                fallback = false;
            } catch (Exception e) {
                // AI 调用异常，降级
                log.warn(warnMessage, e);
                results = fallbackCall.get();
            }
        } else {
            // ChatClient 不存在，直接使用模板兜底
            results = fallbackCall.get();
        }
        return new AiCallResult<>(results, provider, fallback);
    }

    /**
     * 降级开关未开启且实际走了降级时，抛出业务异常（使用默认提示文案）。
     */
    protected void assertNotFallback(boolean fallback) {
        assertNotFallback(fallback, " 当前未启动AI推荐，请先配置模型再试");
    }

    /**
     * 降级开关未开启且实际走了降级时，抛出业务异常。
     *
     * @param message 异常提示文案（各子类语义不同）
     */
    protected void assertNotFallback(boolean fallback, String message) {
        if (!aiProperties.isTemplateFallback() && fallback)
            throw new BusinessException(Result.ERROR_CODE, message);
    }

    protected List<Product> getAllProducts() {
        return productService.getAll();
    }

    /**
     * 将商品列表拼为给 LLM 的文本。
     *
     * @param separator    条目之间的分隔符（搜索用 "、"，推荐用 "\n"）
     * @param includeStock 是否带库存字段
     */
    protected String buildProductList(List<Product> products, String separator, boolean includeStock) {
        return products.stream().map(product -> includeStock
                ? String.format(Locale.ROOT, "ID:%d | %s | %s | %.2f元 | 库存%d |%s",
                product.getId(),
                product.getName(),
                CATEGORY_LABELS.getOrDefault(product.getCategoryId(), "未分类"),
                product.getPrice(),
                product.getStock(),
                StringUtils.hasText(product.getDescription()) ? product.getDescription() : "无描述")
                : String.format(Locale.ROOT, "ID:%d | %s | %s | %.2f元 | %s",
                product.getId(),
                product.getName(),
                CATEGORY_LABELS.getOrDefault(product.getCategoryId(), "未分类"),
                product.getPrice(),
                StringUtils.hasText(product.getDescription()) ? product.getDescription() : "无描述")
        ).collect(Collectors.joining(separator));
    }

    /**
     * 解析 LLM 返回的 JSON 数组（兼容标准 JSON 与 ```json 代码块包裹）。
     * 每个元素提取 id / reason / score 三个字段，按 id 匹配商品后交给 mapper 映射。
     */
    protected <T> List<T> parseAiJson(
            String json,
            List<Product> products,
            BiFunction<Product, ParsedItem, T> mapper) {
        List<T> results = new ArrayList<>();
        if (!StringUtils.hasText(json)) {
            return results;
        }
        // 清理 markdown 标记（```json ... ```）
        String cleaned = json.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "").trim();
        }
        // 截取第一个 [ 到最后一个 ]，容忍模型在 JSON 前后多说的废话
        int start = cleaned.indexOf('[');
        int end = cleaned.lastIndexOf(']');
        if (start != -1 && end != -1 && end > start) {
            cleaned = cleaned.substring(start, end + 1);
        }
        try {
            List<?> list = objectMapper.readValue(cleaned, List.class);
            for (Object obj : list) {
                if (!(obj instanceof Map<?, ?> m)) continue;
                Long id = toLong(m.get("id"));
                if (id == null) continue;
                String reason = m.get("reason") != null ? m.get("reason").toString() : null;
                Integer score = toInt(m.get("score"));
                final Long matchedId = id;
                Product product = products.stream()
                        .filter(p -> p.getId().equals(matchedId))
                        .findFirst().orElse(null);
                if (product != null) {
                    results.add(mapper.apply(product, new ParsedItem(id, reason, score)));
                }
            }
        } catch (Exception e) {
            log.warn("AI 返回 JSON 解析失败，返回空结果：{}", e.getMessage());
        }
        return results;
    }

    /**
     * 单个 token 是否命中商品（名称/描述/分类标签，忽略大小写）。
     */
    protected boolean tokenMatches(Product product, String word) {
        String name = product.getName() == null ? "" : product.getName().toLowerCase();
        String desc = product.getDescription() == null ? "" : product.getDescription().toLowerCase();
        String category = CATEGORY_LABELS.getOrDefault(product.getCategoryId(), "").toLowerCase();
        return name.contains(word) || desc.contains(word) || category.contains(word);
    }

    /**
     * AI 调用结果的统一载体
     */
    protected static class AiCallResult<T> {
        private final List<T> results;
        private final String provider;
        private final boolean fallback;

        AiCallResult(List<T> results, String provider, boolean fallback) {
            this.results = results;
            this.provider = provider;
            this.fallback = fallback;
        }

        public List<T> getResults() {
            return results;
        }

        public String getProvider() {
            return provider;
        }

        public boolean isFallback() {
            return fallback;
        }
    }

    /**
     * 解析出的单条 LLM 结果
     */
    protected record ParsedItem(Long id, String reason, Integer score) {
    }
}
