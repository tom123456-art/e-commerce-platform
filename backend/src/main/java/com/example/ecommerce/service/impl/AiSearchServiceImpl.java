package com.example.ecommerce.service.impl;

import com.example.ecommerce.config.AiProperties;
import com.example.ecommerce.dto.AiSearchRequest;
import com.example.ecommerce.dto.AiSearchResponse;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.service.AiSearchService;
import com.example.ecommerce.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
        if (chatClient != null){

        }
        return null;
    }

    private List<AiSearchResponse.AiSearchProduct> doAiSearch(String query){
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
        //return parseSearchResult(completion, products);
        return null;


    }
}
