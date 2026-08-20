package com.example.ecommerce.service.impl;

import com.example.ecommerce.config.AiProperties;
import com.example.ecommerce.dto.AiRecommendRequest;
import com.example.ecommerce.dto.AiRecommendResponse;
import com.example.ecommerce.dto.AiRecommendResponse.AiRecommendItem;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.service.AiRecommendService;
import com.example.ecommerce.service.ProductService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiRecommendServiceImpl extends AbstractAiService implements AiRecommendService {

    public AiRecommendServiceImpl(
            ProductService productService,
            AiProperties aiProperties,
            ObjectProvider<ChatClient.Builder> chatClientProvider,
            Environment env) {
        super(productService, aiProperties, chatClientProvider, env);
    }

    /**
     * @param request
     * @return
     */
    @Override
    public AiRecommendResponse recommend(AiRecommendRequest request) {
        // 获取查询的文本
        String query = request.getQuery().trim();
        AiCallResult<AiRecommendItem> result = executeAiCall(
                () -> doAiRecommend(query, request.getBudget(), request.getCategoryPreference()),
                () -> doFilterRecommend(request),
                "AI推荐失败，将使用模板推荐");
        // 降级、抛异常
        assertNotFallback(result.isFallback());
        // 封装响应
        AiRecommendResponse response = new AiRecommendResponse();
        response.setRecommendations(result.getResults()); // 设置推荐结果
        response.setQuery(query); // 设置原始的查询
        response.setFallback(result.isFallback()); // 标记是否降级
        response.setProvider(result.getProvider()); // 设置提供者名称
        return response;
    }

    /**
     * 降级推荐：预算优先过滤+类别偏好过滤+关键词相关度评分过滤
     *
     * @param request
     * @return
     */
    private List<AiRecommendItem> doFilterRecommend(AiRecommendRequest request) {
        // 获取全部商品
        List<Product> products = getAllProducts();
        // 获取查询关键词并转为小写
        String query = request.getQuery().toLowerCase();
        // 获取预算
        BigDecimal budget = request.getBudget();
        // 获取类别偏好
        String categoryPreference = request.getCategoryPreference();
        return products.stream().filter(
                p -> { // 过滤条件
                    if (budget != null && p.getPrice() != null && p.getPrice().compareTo(budget) > 0)
                        return false; // 超出预算
                    if (StringUtils.hasText(categoryPreference)) {
                        // 如果有类别偏好，则过滤掉不匹配的商品
                        // 获取商品类别标签
                        String label =
                                CATEGORY_LABELS.getOrDefault(p.getCategoryId(), "");
                        return label.contains(categoryPreference) || categoryPreference.contains(label); // 双向不匹配的话就排除掉
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
     *
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
     *
     * @param product 商品
     * @param query   查询
     * @return
     */
    private int calculateRelevance(Product product, String query) {
        // 初始化惩罚值
        int penalty = 0;
        // 分词
        for (String word : query.split("[\\s,，、]+")) {
            if (word.length() < 2) continue; // 跳过单个字符
            if (tokenMatches(product, word)) {
                penalty -= 20; // 越负越相关
            }
        }
        return penalty;
    }

    /**
     * AI推荐
     *
     * @param query              查询文本
     * @param budget             预算
     * @param categoryPreference 类别偏好
     * @return
     */
    private List<AiRecommendItem> doAiRecommend(String query, BigDecimal budget, String categoryPreference) {
        // 获取全部商品
        List<Product> products = getAllProducts();
        String productList = buildProductList(products, "\n", true);
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

        return parseAiJson(completion, products,
                (product, item) -> toRecommendItem(
                        product,
                        item.score() != null ? item.score() : 50,
                        item.reason() != null ? item.reason() : ""));
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
