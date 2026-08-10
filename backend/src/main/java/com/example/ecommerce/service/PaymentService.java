package com.example.ecommerce.service;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentService {
    /** 创建支付链接（真实支付宝 URL 或 Mock URL） */
    String createPayment(String orderNo, BigDecimal amount, String description);

    /** 处理支付宝异步回调（验签 + 金额校验 + 状态更新 + 审计日志） */
    boolean handleCallback(Map<String, String> callbackParams);

    /** Mock 支付回调（开发环境，跳过验签） */
    boolean handleMockCallback(Map<String, String> callbackParams);
}
