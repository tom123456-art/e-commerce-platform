package com.example.ecommerce.dto;

import lombok.Data;

/**
 * 商家经营看板响应 DTO。
 * totalOrders 和 totalRevenue 为预留字段，当前未实现统计逻辑。
 */
@Data
public class MerchantDashboardResponse {

    private long totalProducts;       // 商品总数
    private long activeProducts;      // 在售商品数（status=1）
    private long totalReviews;        // 评论总数
    private long pendingReplies;      // 待回复数（reply 为空）
    private double averageRating;     // 平均评分（1-5，保留 1 位小数）
    private long totalOrders;         // 总订单数（待实现）
    private double totalRevenue;      // 总收入（待实现）
}
