package com.example.ecommerce.service;

import com.example.ecommerce.dto.AiRecommendRequest;
import com.example.ecommerce.dto.AiRecommendResponse;

/**
 * AI智能推荐服务接口
 */
public interface AiRecommendService {
    AiRecommendResponse recommend(AiRecommendRequest request);
}
