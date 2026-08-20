package com.example.ecommerce.service;

import com.example.ecommerce.dto.ShowcaseDailyMetricItem;
import com.example.ecommerce.dto.ShowcaseMetricSummary;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.OrderItem;

import java.util.List;

/**
 * 商品指标服务接口 —— 定义商品行为指标的记录和统计功能。
 *
 * <h3>Service 层"横切关注点"特性</h3>
 * <ul>
 *   <li><b>被多个业务流程调用</b>：ProductService（浏览）、CartService（加购）、PaymentService（支付）</li>
 *   <li><b>服务于多个下游</b>：ShowcaseStrategyService、ProductService、AdminDashboard</li>
 *   <li><b>解耦数据采集与数据分析</b>：采集端只管写入，分析端只管读取，互不干扰</li>
 * </ul>
 *
 * <h3>读写分离设计</h3>
 * - 写入方法（record*）：实时调用，写入存储
 * - 读取方法（summarize*、recent*）：分析时调用，从存储读取并聚合
 * 这种分离使得"采集"和"分析"可独立演进（如未来将采集端改为消息队列异步化）。
 *
 * <h3>版本说明</h3>
 * 本接口是项目中唯一的 ProductMetricService 定义，被 ProductService、CartServiceImpl、
 * PaymentServiceImpl、ShowcaseStrategyServiceImpl 共同依赖。请勿在其他 code 目录中
 * 创建不同方法名的版本，以免造成版本断裂。
 */
public interface ProductMetricService {

    /**
     * 记录商品浏览事件。
     *
     * @param productId 商品 ID
     * @param userId    用户 ID（可为 null，匿名用户也记录）
     * @param source    浏览来源（detail/search/recommend）
     */
    void recordProductView(Long productId, Long userId, String source);

    /**
     * 记录加购事件。
     *
     * @param productId 商品 ID
     * @param quantity  加购数量
     */
    void recordCartAddition(Long productId, Integer quantity);

    /**
     * 记录支付成功事件（遍历订单项，为每个商品记录支付指标）。
     *
     * @param order      订单实体
     * @param orderItems 订单明细列表
     */
    void recordPaymentSuccess(Order order, List<OrderItem> orderItems);

    /**
     * 汇总指定时间窗口的指标（ShowcaseStrategy 核心数据源）。
     *
     * @param days 窗口天数（1-180）
     * @return 指标汇总（含转化率）
     */
    ShowcaseMetricSummary summarizeWindow(int days);

    /**
     * 获取最近 N 天的每日指标明细（趋势图用）。
     *
     * @param limit 天数限制（1-60）
     * @return 每日指标列表（按日期倒序）
     */
    List<ShowcaseDailyMetricItem> recentDailyMetrics(int limit);
}
