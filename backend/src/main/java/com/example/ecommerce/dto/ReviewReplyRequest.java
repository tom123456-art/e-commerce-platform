package com.example.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 商家回复评论请求 DTO。
 */
public class ReviewReplyRequest {

    @NotNull(message = "评论ID不能为空")
    private Long reviewId;

    @NotBlank(message = "回复内容不能为空")
    private String reply;

    // getter/setter（后端 Service 层通过 getXxx() 读取字段值）
    public Long getReviewId() { return reviewId; }
    public void setReviewId(Long reviewId) { this.reviewId = reviewId; }
    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
}
