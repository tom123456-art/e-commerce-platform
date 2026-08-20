package com.example.ecommerce.service;

import com.example.ecommerce.dto.MerchantRegisterRequest;
import com.example.ecommerce.dto.ReviewReplyRequest;
import com.example.ecommerce.dto.StoreRequest;
import com.example.ecommerce.entity.Review;
import com.example.ecommerce.entity.Store;
import com.example.ecommerce.entity.User;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 商家服务接口，提供商家相关的业务逻辑
 */
public interface MerchantService {
    User register(MerchantRegisterRequest request);

    Store getStore(Long merchantId);

    Store updateStore(Long merchantId, StoreRequest request);

    List<Review> getReviews(Long merchantId);

    void replyReviews(Long merchantId, ReviewReplyRequest request);

    void hideReviews(Long merchantId, Long reviewId);
}
