package com.example.ecommerce.service.impl;

import com.example.ecommerce.common.BusinessException;
import com.example.ecommerce.common.Result;
import com.example.ecommerce.config.AiProperties;
import com.example.ecommerce.dto.AiRecommendResponse;
import com.example.ecommerce.dto.AiSearchRequest;
import com.example.ecommerce.dto.AiSearchResponse;
import com.example.ecommerce.dto.AiSearchResponse.AiSearchProduct;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.service.AiSearchService;
import com.example.ecommerce.service.ProductService;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiSearchServiceImpl implements AiSearchService {

    private static final Logger log = LoggerFactory.getLogger(AiSearchServiceImpl.class);

    private static final String TEMPLATE_PROVIDER = "template-mock";

    private static final Map<Integer, String> CATEGORY_LABELS =
            Collections.unmodifiableMap(Map.of(
                    1,"手机数码",2,"电脑办公",3,"智能家电",
                    4,"家居生活",5,"运动户外",6,"影音娱乐"
            ));

    private final ProductService productService;
    private final AiProperties aiProperties;
    private final ChatClient chatClient;
    private final Environment env;

    public AiSearchServiceImpl(
            ProductService productService,
            AiProperties aiProperties,
            ObjectProvider<ChatClient.Builder> chatClientProvider,
            Environment env) {
        this.productService = productService;
        this.aiProperties = aiProperties;
        // 如果Bean不存在的话，返回null
        ChatClient.Builder builder = chatClientProvider.getIfAvailable();
        this.chatClient = builder == null ? null : builder.build();
        this.env = env;
    }
    /**
     * @param request
     * @return
     */
    @Override
    public AiSearchResponse search(AiSearchRequest request) {
        String query = request.getQuery().trim();
        String provider = TEMPLATE_PROVIDER;
        boolean fallback = true;
        List<AiSearchProduct> results; // 搜索结果的列表
        if (chatClient != null){
            try {
                // 尝试AI搜索
                results = doAiSearch(query);
                provider = "spring-ai";
                fallback = false;
            } catch (Exception e) {
                // 如果AI调用异常，降级
                log.warn("AI搜索调用异常，将使用默认模板提供搜索结果", e);
                results = doKeywordSearch(query);
            }
        } else {
            // 如果ChatClient不存在，直接使用模板提供搜素结果
            results = doKeywordSearch(query);
        }
        // 降级、抛异常
        if (!aiProperties.isTemplateFallback() && fallback)
            throw new BusinessException(Result.ERROR_CODE," 当前未启动AI推荐，请先配置模型再试");
        // 封装响应
        AiSearchResponse response = new AiSearchResponse();
        response.setProducts(results); // 设置推荐结果
        response.setQuery(query); // 设置原始的查询
        response.setFallback(fallback); // 标记是否降级
        response.setProvider(provider); // 设置提供者名称
        return response;
    }

    /**
     * 使用关键词搜素，是AI搜素异常的时候的降级方案
     * @param query
     * @return
     */
    private List<AiSearchProduct> doKeywordSearch(String query) {
        String lower = query.toLowerCase();
        List<Product> products = productService.getAll();
        return products.stream().filter(p -> matchesKeyword(p, lower)).limit(5)
                .map(p -> toSearchProduct(p, "关键词匹配"))
                .collect(Collectors.toList());
    }

    /**
     * 判断商品是否匹配关键词
     * @param product 商品
     * @param keyword 关键词
     * @return
     */
    private boolean matchesKeyword(Product product, String keyword) {
        // 整句匹配
        // 商品名
        if (product.getName() != null &&
                product.getName().toLowerCase().contains(keyword))
            return true;
        // 描述
        if (product.getDescription() != null &&
                product.getDescription().toLowerCase().contains(keyword))
            return true;

        // 分类
        String categoryLabels =
                CATEGORY_LABELS.getOrDefault(product.getCategoryId(), "");
        if (categoryLabels.contains(keyword))
            return true;
        // 分词匹配
        // 按照空格拆分查询，如果有任意一个词命中则匹配
        for (String word : keyword.split("\\s+")){
            if (word.length() < 2) continue; // 跳过单字
            if (
                    (product.getName() != null && product.getName().toLowerCase().contains(word))
                            || (product.getDescription() != null && product.getDescription().toLowerCase().contains(word))
                            || (categoryLabels.toLowerCase().contains(word))
            )
                return true;
        }
        return false;
    }

    private List<AiSearchProduct> doAiSearch(String query){
        List<Product> products = productService.getAll();
        String productList = products.stream().map(
                product -> String.format(
                        Locale.ROOT, "ID:%d | %s | %s | %.2f元 | %s",
                        product.getId(),
                        product.getName(),
                        CATEGORY_LABELS.getOrDefault(product.getCategoryId(), "未分类"),
                        product.getPrice(),
                        StringUtils.hasText(product.getDescription()) ? product.getDescription() : "无描述"
                )
        ).collect(Collectors.joining("、"));
        String systemPrompt = "你是一个电商搜索助手。根据用户的自然语言查询，从商品列表中找到最匹配的商品。"
                + "使用JSON数组格式返回结果，每个元素包含id(number)、reason(string,简短说明匹配的理由)"
                + "最多返回5个最匹配的结果，如果没有匹配到则返回空数组[]。只返回JSON，不要其他内容。";
        String userPrompt = "商品列表：\n" + productList + "\n\n用户搜索：" + query;
        // 调用LLM
        String completion = chatClient.prompt().system(systemPrompt).user(userPrompt).call().content();
        // 返回JSON结果 TODO: 解析JSON结果
        return parseSearchResult(completion, products);
    }

    /**
     * 解析LLM返回
     * @param json
     * @param products
     * @return
     */
    private List<AiSearchProduct> parseSearchResult(String json, List<Product> products) {
        List<AiSearchProduct> results = new ArrayList<>();
        if (!StringUtils.hasText(json)){
            return results;
        }
        // 清理markdown标记
        String cleaned = json.trim();

            /*
            ```json
            [
              {
                "id": 1,
                "message": "这是您请求的JSON数组回复示例。",
                "timestamp": "2026-08-19T10:30:00Z"
              },
              {
                "id": 2,
                "message": "您可以按需修改或解析此结构。",
                "timestamp": "2026-08-19T10:30:01Z"
              }
            ]
            ```
            */

        if (cleaned.startsWith("```")){
            cleaned = cleaned.replaceAll("```json\\s*", "")
                    .replaceAll("```\\s", "").trim();
        }
        /*
        [
              {
                "id": 1,
                "message": "这是您请求的JSON数组回复示例。",
                "timestamp": "2026-08-19T10:30:00Z"
              },
              {
                "id": 2,
                "message": "您可以按需修改或解析此结构。",
                "timestamp": "2026-08-19T10:30:01Z"
              }
            ]
        */
        cleaned = cleaned.replace("[", "").replace("]", "");
        /*
            entries[0] = {
                "id": 1,
                "message": "这是您请求的JSON数组回复示例。",
                "timestamp": "2026-08-19T10:30:00Z"
            entries[1] = "id": 2,
                "message": "您可以按需修改或解析此结构。",
                "timestamp": "2026-08-19T10:30:01Z"
              }
         */
        String[] entries = cleaned.split("\\},\\s*\\{");
        for (String entry : entries){
            String item = entry.replace("{", "").replace("}", "");
            if (!StringUtils.hasText(item)) continue;
            Long id = null;
            String reason = null;
            // 解析item
            for (String pair : item.split("，")){
                // 解析key-value，limit的意思是最多分割成2部分
                String[] keyValue = pair.split(":", 2);
                if (keyValue.length != 2) continue;
                String key = keyValue[0].trim().replace("\"", "");
                String value = keyValue[1].trim().replace("\"", "");
                if ("id".equals(key)){
                    try {
                        id = Long.parseLong(value);
                    } catch (NumberFormatException e){

                    }
                } else if ("reason".equals(key)){
                    reason = value;
                }
            }
            // 根据id找到对应的商品
            if (id != null) {
                final Long matchedId = id;
                Product product = products.stream().filter(
                                p -> p.getId().equals(matchedId))
                        .findFirst().orElse(null);
                if (product != null)
                    results.add(toSearchProduct(product, reason));
            }
        }
        return results;
    }

    private AiSearchProduct toSearchProduct(Product product, String reason) {
        // Product->AiSearchProduct
        AiSearchProduct asp = new AiSearchProduct();
        asp.setId(product.getId());
        asp.setName(product.getName());
        asp.setCategory(product.getCategoryId());
        asp.setPrice(product.getPrice());
        asp.setReason(reason);
        return asp;
    }
}
