package com.example.ecommerce.service;

import com.example.ecommerce.dto.AiSearchRequest;
import com.example.ecommerce.dto.AiSearchResponse;

public interface AiSearchService {
    AiSearchResponse search(AiSearchRequest request);
}
