package com.example.ecommerce.service.impl;

import com.example.ecommerce.config.AiProperties;
import com.example.ecommerce.dto.AiSearchRequest;
import com.example.ecommerce.dto.AiSearchResponse;
import com.example.ecommerce.dto.AiSearchResponse.AiSearchProduct;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.service.AiSearchService;
import com.example.ecommerce.service.ProductService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiSearchServiceImpl extends AbstractAiService implements AiSearchService {

    public AiSearchServiceImpl(
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
    public AiSearchResponse search(AiSearchRequest request) {
        String query = request.getQuery().trim();
        AiCallResult<AiSearchProduct> result = executeAiCall(
                () -> doAiSearch(query),
                () -> doKeywordSearch(query),
                "AI搜索调用异常，将使用默认模板提供搜索结果");
        // 降级、抛异常
        assertNotFallback(result.isFallback());
        // 封装响应
        AiSearchResponse response = new AiSearchResponse();
        response.setProducts(result.getResults()); // 设置推荐结果
        response.setQuery(query); // 设置原始的查询
        response.setFallback(result.isFallback()); // 标记是否降级
        response.setProvider(result.getProvider()); // 设置提供者名称
        return response;
    }

    /**
     * 使用关键词搜素，是AI搜素异常的时候的降级方案
     *
     * @param query
     * @return
     */
    private List<AiSearchProduct> doKeywordSearch(String query) {
        String lower = query.toLowerCase();
        List<Product> products = getAllProducts();
        return products.stream().filter(p -> matchesKeyword(p, lower)).limit(5)
                .map(p -> toSearchProduct(p, "关键词匹配"))
                .collect(Collectors.toList());
    }

    /**
     * 判断商品是否匹配关键词
     *
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
        for (String word : keyword.split("\\s+")) {
            if (word.length() < 2) continue; // 跳过单字
            if (tokenMatches(product, word))
                return true;
        }
        return false;
    }

    private List<AiSearchProduct> doAiSearch(String query) {
        List<Product> products = getAllProducts();
        String productList = buildProductList(products, "、", false);
        String systemPrompt = "你是一个电商搜索助手。根据用户的自然语言查询，从商品列表中找到最匹配的商品。"
                + "使用JSON数组格式返回结果，每个元素包含id(number)、reason(string,简短说明匹配的理由)"
                + "最多返回5个最匹配的结果，如果没有匹配到则返回空数组[]。只返回JSON，不要其他内容。";
        String userPrompt = "商品列表：\n" + productList + "\n\n用户搜索：" + query;
        // 调用LLM
        String completion = chatClient.prompt().system(systemPrompt).user(userPrompt).call().content();
        // 返回JSON结果 TODO: 解析JSON结果
        return parseAiJson(completion, products,
                (product, item) -> toSearchProduct(product, item.reason()));
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
