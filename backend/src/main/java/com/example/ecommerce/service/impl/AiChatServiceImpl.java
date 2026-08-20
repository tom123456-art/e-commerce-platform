package com.example.ecommerce.service.impl;

import com.example.ecommerce.config.AiProperties;
import com.example.ecommerce.dto.AiChatRequest;
import com.example.ecommerce.dto.AiChatResponse;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.service.AiChatService;
import com.example.ecommerce.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class AiChatServiceImpl extends AbstractAiService implements AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatServiceImpl.class);

    // 降级模式下提供的provider/model标识
    private static final String TEMPLATE_MODEL = "chat-template";

    // 会话历史窗口大小，保留最近10轮
    private static final int SESSION_HISTORY_SIZE = 10;

    // 会话历史缓存，key为会话id，value为会话历史记录列表
    private final ConcurrentHashMap<String, List<String[]>> sessions
            = new ConcurrentHashMap<>();

    public AiChatServiceImpl(
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
    public AiChatResponse chat(AiChatRequest request) {
        // 解析sessionId,如果前端传过来了就用，不然就生成一个新的
        String sessionId = resolveSessionId(request.getSessionId());
        String message = request.getMessage().trim();
        // 读取会话历史记录
        List<String[]> history = sessions.getOrDefault(sessionId, new ArrayList<>());
        // 设置默认走降级模板做兜底回复，如果AI回复成功的话，覆盖即可
        String reply = buildTemplateReply(message);
        boolean fallback = true;
        String provider = TEMPLATE_PROVIDER;
        String model = TEMPLATE_MODEL;

        // 如果ChatClient可以用
        if (chatClient != null) {
            try {
                String systemPrompt = buildSystemPrompt();
                // 拼接对话上下文
                StringBuilder stringBuilder = new StringBuilder();
                for (String[] entry : history) {
                    stringBuilder.append(entry[0]).append(":").append(entry[1]).append("\n");
                }
                stringBuilder.append("用户:").append(message).append("\n");
                // 调用SpringAi
                String completion = chatClient.prompt().system(systemPrompt).user(stringBuilder.toString())
                        .call().content();
                if (StringUtils.hasText(completion)) {
                    reply = completion.trim();
                    fallback = false;
                    provider = "spring-ai";
                    model = env.getProperty("spring.ai.openai.chat.options.model", "openai");
                }
            } catch (Exception e) {
                log.warn("AI回复失败，将使用模板回复", e);
            }
        }
        // 降级、抛异常
        assertNotFallback(fallback, "当前未启用SpringAI,请先配置模型后再试");
        // 保存本轮对话到历史记录
        saveHistory(sessionId, message, reply);
        // 封装响应
        AiChatResponse response = new AiChatResponse();
        response.setReply(reply);
        response.setSessionId(sessionId);
        response.setFallback(fallback);
        response.setProvider(provider);
        response.setModel(model);

        return response;
    }

    /**
     * 保存会话历史记录
     *
     * @param sessionId
     * @param message
     * @param reply
     */
    private void saveHistory(String sessionId, String message, String reply) {
        List<String[]> history = sessions.getOrDefault(sessionId, new ArrayList<>());
        history.add(new String[]{"用户", message});
        history.add(new String[]{"客服", reply});
        if (history.size() > SESSION_HISTORY_SIZE * 2) {
            history = new ArrayList<>(
                    history.subList(
                            history.size() - SESSION_HISTORY_SIZE * 2,
                            history.size()
                    )
            );
            sessions.put(sessionId, history);
        }
    }

    /**
     * 构建系统提示，用于引导AI的回复方向
     * 让AI知道他是商城的客服，并且了解商城售卖的商品
     *
     * @return
     */
    private String buildSystemPrompt() {
        List<Product> products = getAllProducts();
        String productList = products.stream().limit(20).map(
                product -> String.format(
                        Locale.ROOT, "%s(%s,%.2f元,库存%d)",
                        product.getName(),
                        CATEGORY_LABELS.getOrDefault(product.getCategoryId(), "未分类"),
                        product.getPrice(),
                        product.getStock()
                )
        ).collect(Collectors.joining("、"));
        return "你是商城的AI助手，可以帮助用户解答商品相关问题，例如商品咨询、订单查询、物流跟踪，售后服务等，"
                + "请使用简体中文回答问题，语气亲切专业。"
                + "当前商城的主要商品有：" + productList
                + "如果用户查询的商品不在列表中，请告知并推荐类似的商品，对于订单、物流等非商品问题，" +
                "请给出通用建议";
    }

    /**
     * 模板降级回复，根据关键词匹配预设的回答，这是兜底使用的回复
     *
     * @param message
     * @return
     */
    private String buildTemplateReply(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("推荐") || lower.contains("有什么") || lower.contains("什么好")) {
            List<Product> products = getAllProducts();
            if (!products.isEmpty()) {
                String items = products.stream().limit(3)
                        .map(product -> product.getName()
                                + "("
                                + CATEGORY_LABELS.getOrDefault(product.getCategoryId(), "未分类")
                                + ")"
                        ).collect(Collectors.joining("、"));
                return "推荐商品：" + items;
            }
            return "没有找到相关商品";
        }
        // 其他关键词匹配
        // 咨询订单、物流、快递
        if (lower.contains("订单") || lower.contains("物流") || lower.contains("快递"))
            return "您可以在[我的]-[我的订单]查看订单状态、物流信息等，如需其他帮助，请转人工客服";

        // 咨询退货、换货、售后
        if (lower.contains("退货") || lower.contains("换货") || lower.contains("售后"))
            return "您可以在[我的]-[售后]查看退货、换货信息等，如需其他帮助，请转人工客服";

        // 问候语
        if (lower.contains("你好") || lower.contains("hello") || lower.contains("hi"))
            return "你好，欢迎来到商城，请问有什么可以帮助您的吗？";
        // 其他问题
        return "感谢您的咨询，我是商城的AI助手，请问有什么可以帮助您的吗？";
    }

    private String resolveSessionId(String sessionId) {
        if (StringUtils.hasText(sessionId)) {
            return sessionId.trim();
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
