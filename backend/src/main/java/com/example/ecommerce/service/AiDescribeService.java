package com.example.ecommerce.service;

import com.example.ecommerce.dto.AiDescribeRequest;
import com.example.ecommerce.dto.AiDescribeResponse;

/**
 * AI生成商品文案
 */
public interface AiDescribeService {
    AiDescribeResponse generateDescription(AiDescribeRequest request);
}
