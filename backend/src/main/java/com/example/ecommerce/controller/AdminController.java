package com.example.ecommerce.controller;

import com.example.ecommerce.common.Result;
import com.example.ecommerce.config.ShowcaseProperties;
import com.example.ecommerce.dto.AdminDashboardResponse;
import com.example.ecommerce.dto.DashboardChartItem;
import com.example.ecommerce.dto.ProductShowcaseResponse;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.service.AdminDashboardVisualizationService;
import com.example.ecommerce.service.OrderService;
import com.example.ecommerce.service.ProductService;
import com.example.ecommerce.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "管理员后台接口", description = "管理员商品、订单、用户管理")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final ProductService productService;
    private final OrderService orderService;

    private final ShowcaseProperties showcaseProperties;
    private final AdminDashboardVisualizationService adminDashboardVisualizationService;

    public AdminController(UserService userService, ProductService productService, OrderService orderService, ShowcaseProperties showcaseProperties, AdminDashboardVisualizationService adminDashboardVisualizationService) {
        this.userService = userService;
        this.productService = productService;
        this.orderService = orderService;
        this.showcaseProperties = showcaseProperties;
        this.adminDashboardVisualizationService = adminDashboardVisualizationService;
    }

    @GetMapping("/dashboard")
    public Result<AdminDashboardResponse> dashboard(){
        return Result.success(buildDashboardResponse());
    }


    @GetMapping(value = "/dashboard/visualization", produces = MediaType.TEXT_HTML_VALUE)
    public String dashboardVisualization() {
        return adminDashboardVisualizationService.renderHtml(buildDashboardResponse());
    }

    /**
     * 构建仪表盘的响应对象，可以聚合User、Product、Order的数据
     * @return
     */
    private AdminDashboardResponse buildDashboardResponse() {
        // 1、查询原始数据
        List<User> users = userService.getAll();
        List<Product> products = productService.getAll();
        List<Order> orders = orderService.getAll();
        // 2、统计个状态的订单数
        // 待支付
        long pendingOrderCount = orders.stream().filter(
                order -> order.getStatus() != null && order.getStatus() == 0
        ).count();
        // 已支付
        long paidOrderCount = orders.stream().filter(
                order -> order.getStatus() != null && order.getStatus() == 1
        ).count();

        // 已完成
        long completedOrderCount = orders.stream().filter(
                order -> order.getStatus() != null && order.getStatus() == 2
        ).count();
        // 3、组装响应对象
        AdminDashboardResponse response = new AdminDashboardResponse();
        response.setUserCount(users.size());
        response.setProductCount(products.size());
        response.setOrderCount(orders.size());
        response.setPendingOrderCount(pendingOrderCount);
        response.setPaidOrderCount(paidOrderCount);
        response.setCompletedOrderCount(completedOrderCount);
        // 判断库存数量少于一定值之后，提醒管理员补货
        response.setLowStockProductCount(
                products.stream().filter(
                        product -> product.getStock() != null && product.getStock() <= 10
                ).count()
        );
        // 4、构建各种图表
        // 订单
        response.setOrderStatusDistribution(
                buildOrderStatusDistribution(pendingOrderCount, paidOrderCount, completedOrderCount)
        );
        // 商品分类
        response.setProductCategoryDistribution(
                buildProductCategoryDistribution(products)
        );
        // 用户角色
        response.setUserRoleDistribution(
                buildUserRoleDistribution(users)
        );
        // 用户状态
        response.setUserStatusDistribution(
                buildUserStatusDistribution(users)
        );
        // 热销产品
        response.setHotProductDistribution(
                buildHotProductDistribution(products)
        );
        // 推荐产品
        response.setRecommendedProductDistribution(
                buildRecommendedProductDistribution(products)
        );
        // 推荐权重
        response.setRecommendationWeightDistribution(
                buildRecommendationWeightDistribution(products)
        );
        return response;
    }

    /**
     * 构建推荐权重分布饼图
     * @param products
     * @return
     */
    private List<DashboardChartItem> buildRecommendationWeightDistribution(List<Product> products) {
        List<DashboardChartItem> items = new ArrayList<>();
        ShowcaseProperties.PersonalizedWeights personalized = showcaseProperties.getPersonalized();
        items.add(new DashboardChartItem("品类偏好", personalized.getCategory()));
        items.add(new DashboardChartItem("热度", personalized.getHot()));
        items.add(new DashboardChartItem("价格带", personalized.getPrice()));
        items.add(new DashboardChartItem("新鲜度", personalized.getFreshness()));
        items.add(new DashboardChartItem("库存量", personalized.getInventory()));
        return items;
    }

    /**
     * 构建推荐产品分布饼图
     * @param products
     * @return
     */
    private List<DashboardChartItem> buildRecommendedProductDistribution(List<Product> products) {
        List<ProductShowcaseResponse> recommendedProducts
                = productService.getRecommendedProducts(null, 6);
        List<DashboardChartItem> items = new ArrayList<>();
        for (ProductShowcaseResponse recommended: recommendedProducts){
            if (recommended.getProduct() == null) continue;
            items.add(
                    new DashboardChartItem(
                            recommended.getProduct().getName(),
                            recommended.getScore() == null ? 0D : recommended.getScore()
                    )
            );
        }
        return items;

    }

    /**
     * 构建热销产品分布饼图
     * @param products
     * @return
     */
    private List<DashboardChartItem> buildHotProductDistribution(List<Product> products) {
        List<ProductShowcaseResponse> hotProducts = productService.getHotProducts(6);
        List<DashboardChartItem> items = new ArrayList<>();
        for (ProductShowcaseResponse hot: hotProducts){
            if (hot.getProduct() == null) continue;
            items.add(new DashboardChartItem(
                    hot.getProduct().getName(),
                    hot.getScore() == null ? 0D : hot.getScore()
            ));
        }
        return items;
    }

    /**
     * 构建用户状态分布饼图
     * @param users
     * @return
     */
    private List<DashboardChartItem> buildUserStatusDistribution(List<User> users) {
        Map<String, Double> buckets = new LinkedHashMap<>();
        buckets.put("启用", 0D);
        buckets.put("禁用", 0D);
        for (User user : users){
            String label = user.getStatus() != null && user.getStatus() == 1 ? "启用" : "禁用";
            buckets.put(label, buckets.get(label) + 1D);
        }
        return toChartItems(buckets);
    }

    /**
     * 构建用户角色分布图
     * @param users
     * @return
     */
    private List<DashboardChartItem> buildUserRoleDistribution(List<User> users) {
        Map<String, Double> buckets = new LinkedHashMap<>();
        buckets.put("管理员", 0D);
        buckets.put("普通用户", 0D);
        for (User user : users){
            String label = "ADMIN".equalsIgnoreCase(user.getRole()) ? "管理员": "普通用户";
            buckets.put(label, buckets.get(label) + 1D);
        }
        return toChartItems(buckets);
    }

    /**
     * 构建订单状态分布饼图数据
     * @param pendingOrderCount
     * @param paidOrderCount
     * @param completedOrderCount
     * @return
     */
    private List<DashboardChartItem> buildOrderStatusDistribution(long pendingOrderCount, long paidOrderCount, long completedOrderCount) {
        List<DashboardChartItem> items = new ArrayList<>();
        items.add(new DashboardChartItem("待支付", (double) pendingOrderCount));
        items.add(new DashboardChartItem("已支付", (double) paidOrderCount));
        items.add(new DashboardChartItem("已完成", (double) completedOrderCount));
        return items;
    }

    /**
     * 构建产品分类分布饼图数据
     * @param products
     * @return
     */
    private List<DashboardChartItem> buildProductCategoryDistribution(List<Product> products){
        Map<String, Double> buckets = new LinkedHashMap<>();
        for (Product product: products){
            Integer categoryId = product.getCategoryId();
            String label = categoryLabel(categoryId);
            // 如果包含该标签，则累加，否则新增
            buckets.put(label, buckets.containsKey(label)? buckets.get(label) + 1D : 1D);
        }
        return toChartItems(buckets);
    }

    /**
     * 将Map<String,Double>转换成图表的数据列表
     * @param buckets
     * @return
     */
    private List<DashboardChartItem> toChartItems(Map<String, Double> buckets) {
        List<DashboardChartItem> items = new ArrayList<>();
        for (Map.Entry<String, Double> entry : buckets.entrySet()) {
            items.add(new DashboardChartItem(entry.getKey(), entry.getValue()));
        }
        return items;
    }

    /**
     * 根据分类ID获取分类标签
     * @param categoryId
     * @return
     */
    private String categoryLabel(Integer categoryId) {
        if (categoryId == null) return "未分类";
        switch (categoryId){
            case 1:
                return "手机数码";
            case 2:
                return "电脑办公";
            case 3:
                return "智能家电";
            case 4:
                return "家居生活";
            case 5:
                return "运动户外";
            case 6:
                return "手机数码";
            default:
                return "未分类";
        }
    }
}
