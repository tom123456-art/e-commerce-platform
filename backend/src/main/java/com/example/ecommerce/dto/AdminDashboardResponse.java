package com.example.ecommerce.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理后台仪表盘响应数据 —— 响应 DTO 的"聚合数据"模式。
 *
 * <h3>按页面/视图聚合数据</h3>
 * <p>仪表盘页面需要一次性展示多种统计数据。若每种数据单独调接口，
 * 会产生多次 HTTP 请求，增加页面加载时间。本 DTO 将管理员需要的
 * 所有统计数据聚合到一个对象中，前端一次请求即可获取全部数据。</p>
 *
 * <h3>List 字段初始化为 new ArrayList 的原因</h3>
 * <p>所有 List 字段在声明时初始化为空列表而非 null。
 * 这样前端收到 JSON 后可直接遍历，无需做 null 判断：
 * {@code data.orderStatusDistribution.forEach(...)} 不会抛 TypeError。</p>
 */
public class AdminDashboardResponse {

    // ========== 核心统计指标 ==========

    /**
     * 注册用户总数
     */
    private long userCount;

    /**
     * 商品总数
     */
    private long productCount;

    /**
     * 订单总数
     */
    private long orderCount;

    /**
     * 待处理订单数（待支付或待发货），提醒管理员及时跟进
     */
    private long pendingOrderCount;

    /**
     * 已支付订单数
     */
    private long paidOrderCount;

    /**
     * 已完成订单数
     */
    private long completedOrderCount;

    /**
     * 低库存商品数量（库存 <= 10），提醒管理员补货
     */
    private long lowStockProductCount;

    // ========== 图表分布数据（均初始化为空列表） ==========

    /**
     * 订单状态分布（饼图）：待支付/已支付/已完成
     */
    private List<DashboardChartItem> orderStatusDistribution = new ArrayList<DashboardChartItem>();

    /**
     * 商品分类分布（柱状图）
     */
    private List<DashboardChartItem> productCategoryDistribution = new ArrayList<DashboardChartItem>();

    /**
     * 用户角色分布（饼图）：管理员/普通用户
     */
    private List<DashboardChartItem> userRoleDistribution = new ArrayList<DashboardChartItem>();

    /**
     * 用户状态分布（饼图）：正常/禁用
     */
    private List<DashboardChartItem> userStatusDistribution = new ArrayList<DashboardChartItem>();

    /**
     * 热销商品 Top N（柱状图）
     */
    private List<DashboardChartItem> hotProductDistribution = new ArrayList<DashboardChartItem>();

    /**
     * 推荐商品分布
     */
    private List<DashboardChartItem> recommendedProductDistribution = new ArrayList<DashboardChartItem>();

    /**
     * 推荐权重分布（雷达图）
     */
    private List<DashboardChartItem> recommendationWeightDistribution = new ArrayList<DashboardChartItem>();

    // Getter / Setter 省略（与字段一一对应）...
    public long getUserCount() {
        return userCount;
    }

    public void setUserCount(long userCount) {
        this.userCount = userCount;
    }

    public long getProductCount() {
        return productCount;
    }

    public void setProductCount(long productCount) {
        this.productCount = productCount;
    }

    public long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(long orderCount) {
        this.orderCount = orderCount;
    }

    public long getPendingOrderCount() {
        return pendingOrderCount;
    }

    public void setPendingOrderCount(long pendingOrderCount) {
        this.pendingOrderCount = pendingOrderCount;
    }

    public long getPaidOrderCount() {
        return paidOrderCount;
    }

    public void setPaidOrderCount(long paidOrderCount) {
        this.paidOrderCount = paidOrderCount;
    }

    public long getCompletedOrderCount() {
        return completedOrderCount;
    }

    public void setCompletedOrderCount(long completedOrderCount) {
        this.completedOrderCount = completedOrderCount;
    }

    public long getLowStockProductCount() {
        return lowStockProductCount;
    }

    public void setLowStockProductCount(long lowStockProductCount) {
        this.lowStockProductCount = lowStockProductCount;
    }

    public List<DashboardChartItem> getOrderStatusDistribution() {
        return orderStatusDistribution;
    }

    public void setOrderStatusDistribution(List<DashboardChartItem> orderStatusDistribution) {
        this.orderStatusDistribution = orderStatusDistribution;
    }

    public List<DashboardChartItem> getProductCategoryDistribution() {
        return productCategoryDistribution;
    }

    public void setProductCategoryDistribution(List<DashboardChartItem> productCategoryDistribution) {
        this.productCategoryDistribution = productCategoryDistribution;
    }

    public List<DashboardChartItem> getUserRoleDistribution() {
        return userRoleDistribution;
    }

    public void setUserRoleDistribution(List<DashboardChartItem> userRoleDistribution) {
        this.userRoleDistribution = userRoleDistribution;
    }

    public List<DashboardChartItem> getUserStatusDistribution() {
        return userStatusDistribution;
    }

    public void setUserStatusDistribution(List<DashboardChartItem> userStatusDistribution) {
        this.userStatusDistribution = userStatusDistribution;
    }

    public List<DashboardChartItem> getHotProductDistribution() {
        return hotProductDistribution;
    }

    public void setHotProductDistribution(List<DashboardChartItem> hotProductDistribution) {
        this.hotProductDistribution = hotProductDistribution;
    }

    public List<DashboardChartItem> getRecommendedProductDistribution() {
        return recommendedProductDistribution;
    }

    public void setRecommendedProductDistribution(List<DashboardChartItem> recommendedProductDistribution) {
        this.recommendedProductDistribution = recommendedProductDistribution;
    }

    public List<DashboardChartItem> getRecommendationWeightDistribution() {
        return recommendationWeightDistribution;
    }

    public void setRecommendationWeightDistribution(List<DashboardChartItem> recommendationWeightDistribution) {
        this.recommendationWeightDistribution = recommendationWeightDistribution;
    }
}
