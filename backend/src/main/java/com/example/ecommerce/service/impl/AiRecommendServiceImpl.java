package com.example.ecommerce.service.impl;

import com.example.ecommerce.common.BusinessException;
import com.example.ecommerce.common.Result;
import com.example.ecommerce.config.AiProperties;
import com.example.ecommerce.dto.AiRecommendRequest;
import com.example.ecommerce.dto.AiRecommendResponse;
import com.example.ecommerce.dto.AiRecommendResponse.AiRecommendItem;
import com.example.ecommerce.dto.AiSearchResponse;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.service.AiRecommendService;
import com.example.ecommerce.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiRecommendServiceImpl implements AiRecommendService {

    private static final Logger log = LoggerFactory.getLogger(AiRecommendServiceImpl.class);

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

    public AiRecommendServiceImpl(
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
    public AiRecommendResponse recommend(AiRecommendRequest request) {
        // 获取查询的文本
        String query = request.getQuery().trim();
        String provider = TEMPLATE_PROVIDER;
        boolean fallback = true; // 默认是模板
        List<AiRecommendItem> results; // 结果列表
        if (chatClient != null) { // AI可用
            try{
                results = doAiRecommend(query, request.getBudget(), request.getCategoryPreference());
                provider = "spring-ai";
                fallback = false;
            } catch (Exception e){
                log.warn("AI推荐失败，将使用模板推荐", e);
                results = doFilterRecommend(request);
            }
        } else {
            results = doFilterRecommend(request);
        }
        // 降级、抛异常
        if (!aiProperties.isTemplateFallback() && fallback)
            throw new BusinessException(Result.ERROR_CODE," 当前未启动AI推荐，请先配置模型再试");
        // 封装响应
        AiRecommendResponse response = new AiRecommendResponse();
        response.setRecommendations(results); // 设置推荐结果
        response.setQuery(query); // 设置原始的查询
        response.setFallback(fallback); // 标记是否降级
        response.setProvider(provider); // 设置提供者名称
        return response;
    }

    /**
     * 降级推荐：预算优先过滤+类别偏好过滤+关键词相关度评分过滤
     * @param request
     * @return
     */
    private List<AiRecommendItem> doFilterRecommend(AiRecommendRequest request) {
        // 获取全部商品
        List<Product> products = productService.getAll();
        // 获取查询关键词并转为小写
        String query = request.getQuery().toLowerCase();
        // 获取预算
        BigDecimal budget = request.getBudget();
        // 获取类别偏好
        String categoryPreference = request.getCategoryPreference();
//        List<Product> filtered = new ArrayList<>();
//        for (Product p : products){
//            if (budget != null && p.getPrice() != null && p.getPrice().compareTo(budget) > 0)
//                continue; // 超出预算
//            if (StringUtils.hasText(categoryPreference)){
//                // 如果有类别偏好，则过滤掉不匹配的商品
//                // 获取商品类别标签
//                String label = CATEGORY_LABELS.getOrDefault(p.getCategoryId(), "");
//                if (!label.contains(categoryPreference) && !categoryPreference.contains(label))
//                    continue; // 双向不匹配的话就排除掉
//            }
//            filtered.add(p); // 符合条件的商品添加到结果列表
//        }
//        filtered.sort(Comparator.comparingInt(p -> calculateRelevance(p, query)));
//        // 存放最终推荐结果集
//        List<AiRecommendItem> results = new ArrayList<>();
//        int count = Math.min(5, filtered.size());
//        // 遍历推荐的所有商品
//        for (int i = 0; i < count; i++){
//            // 取出推荐的商品
//            Product product = filtered.get(i);
//            int score = Math.max(60, 100 - calculateRelevance(product, query));
//            results.add(toRecommendItem(product, score, buildFilterReason(product, query)))
//        }
//        return results;
        return products.stream().filter(
                p -> { // 过滤条件
                    if (budget != null && p.getPrice() != null && p.getPrice().compareTo(budget) > 0)
                        return false; // 超出预算
                    if (StringUtils.hasText(categoryPreference)){
                        // 如果有类别偏好，则过滤掉不匹配的商品
                        // 获取商品类别标签
                        String label =
                                CATEGORY_LABELS.getOrDefault(p.getCategoryId(), "");
                        if (!label.contains(categoryPreference) && !categoryPreference.contains(label))
                            return false; // 双向不匹配的话就排除掉
                    }
                    return true; // 保留该商品
                }).sorted(
                        // 按照相关度评分排序
                        Comparator.comparingInt(p -> calculateRelevance(p, query))
        ).limit(5).map(
                // 映射为推荐项
                p -> {
                    // 最低分60，评分= 100 - 相关度惩罚
                    int score = Math.max(60, 100 - calculateRelevance(p, query));
                    return toRecommendItem(p, score, buildFilterReason(p, query));
                }
        ).collect(Collectors.toList());
    }

    /**
     * 构建降级推荐理由文本
     * @param p
     * @param query
     * @return
     */
    private String buildFilterReason(Product p, String query) {
        // 获取分类标签
        String categoryLabel = CATEGORY_LABELS.getOrDefault(
                p.getCategoryId(), "未分类"
        );
        return "基于您的需求，为您推荐" + categoryLabel + "类商品" + p.getName() + "。";
    }

    /**
     * 计算商品和查询的商品的相关度，返回惩罚值，越小越相关
     * 每当命中一个关键词分数+20（惩罚-20）
     * @param product 商品
     * @param query 查询
     * @return
     */
    private int calculateRelevance(Product product, String query) {
        // 初始化惩罚值
        int penalty = 0;
        String name = product.getName() == null ? "" :
                product.getName().toLowerCase();
        String desc = product.getDescription() == null ? "" :
                product.getDescription().toLowerCase();
        String category = CATEGORY_LABELS.getOrDefault(
                product.getCategoryId(), ""
        ).toLowerCase();
        // 分词
        for (String word : query.split("[\\s,，、]+")){
            if (word.length() < 2) continue; // 跳过单个字符
            if (name.contains(word) || desc.contains(word) || category.contains(word)){
                penalty -= 20; // 越负越相关
            }
        }
        return penalty;
    }

    /**
     * AI推荐
     * @param query 查询文本
     * @param budget 预算
     * @param categoryPreference  类别偏好
     * @return
     */
    private List<AiRecommendItem> doAiRecommend(String query, BigDecimal budget, String categoryPreference) {
        // 获取全部商品
        List<Product> products = productService.getAll();
        String productList = products.stream().map(
                product -> String.format(
                        Locale.ROOT, "ID:%d | %s | %s | %.2f元 | 库存%d |%s",
                        product.getId(),
                        product.getName(),
                        CATEGORY_LABELS.getOrDefault(product.getCategoryId(), "未分类"),
                        product.getPrice(),
                        product.getStock(),
                        StringUtils.hasText(product.getDescription()) ? product.getDescription() : "无描述"
                )
        ).collect(Collectors.joining("\n"));
        String systemPrompt = "你是一个电商选品专家。根据用户的需求描述，从商品列表中推荐最合适的商品。"
                + "请用JSON数组格式返回结果，每个元素包含id(number)、score(number), 1-100推荐评分、" +
                "reason(string), 推荐理由；" +
                "最多返回5个推荐商品，按照推荐评分从高到低排列。只返回JSON，不要其他内容。";
        // 拼接用户的需求、预算、偏好
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("商品列表：\n").append(productList);
        userPrompt.append("\n\n用户需求：").append(query);
        if (budget != null)
            userPrompt.append("\n预算：").append(budget).append("元以内");
        if (StringUtils.hasText(categoryPreference))
            userPrompt.append("\n偏好分类：").append(categoryPreference);
        String completion = chatClient.prompt().
                system(systemPrompt)
                .user(userPrompt.toString())
                .call()
                .content();

        return parseRecommendResults(completion, products);
    }

    private List<AiRecommendItem> parseRecommendResults(String json, List<Product> products) {
        List<AiRecommendItem> results = new ArrayList<>();
        if (!StringUtils.hasText(json)){
            return results;
        }
        // 清理markdown标记
        String cleaned = json.trim();
        if (cleaned.startsWith("```")){
            cleaned = cleaned.replaceAll("```json\\s*", "")
                    .replaceAll("```\\s", "").trim();
        }
        cleaned = cleaned.replace("[", "").replace("]", "");
        String[] entries = cleaned.split("\\},\\s*\\{");
        for (String entry : entries){
            String item = entry.replace("{", "").replace("}", "");
            if (!StringUtils.hasText(item)) continue;
            Long id = null;
            Integer score = 50; //默认分数
            String reason = "";
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
                    // 转换并添加到结果列表
                    results.add(toRecommendItem(product,score, reason));
            }
        }
        return results;
    }

    private AiRecommendItem toRecommendItem(Product product, Integer score, String reason) {
        AiRecommendItem asp = new AiRecommendItem();
        asp.setId(product.getId());
        asp.setName(product.getName());
        asp.setCategory(product.getCategoryId());
        asp.setPrice(product.getPrice());
        asp.setScore(score);
        asp.setReason(reason);
        return asp;
    }
}
