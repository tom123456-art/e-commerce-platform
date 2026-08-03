package com.example.ecommerce.service.impl;

import com.example.ecommerce.dto.ShowcaseDailyMetricItem;
import com.example.ecommerce.dto.ShowcaseMetricSummary;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.OrderItem;
import com.example.ecommerce.entity.ProductMetricDaily;
import com.example.ecommerce.entity.ProductViewEvent;
import com.example.ecommerce.mapper.ProductMetricDailyMapper;
import com.example.ecommerce.mapper.ProductViewEventMapper;
import com.example.ecommerce.service.ProductMetricService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 商品指标服务实现类 —— 数据驱动的运营支撑。
 *
 * <h3>职责</h3>
 * <ul>
 *   <li><b>recordProductView</b>：记录商品浏览事件（写入事件表 + 更新每日汇总）</li>
 *   <li><b>recordCartAddition</b>：记录加购事件（更新每日汇总表的 cart_add_count）</li>
 *   <li><b>recordPaymentSuccess</b>：记录支付成功事件（遍历订单项，更新每日汇总表的支付指标）</li>
 *   <li><b>summarizeWindow</b>：查询指标汇总（从 product_metric_daily 表聚合查询）</li>
 *   <li><b>recentDailyMetrics</b>：查询最近 N 天的每日指标（用于趋势图）</li>
 * </ul>
 *
 * <h3>设计模式</h3>
 * <ul>
 *   <li><b>事件驱动</b>：其他 Service 在关键业务节点（浏览/加购/支付）调用本服务</li>
 *   <li><b>增量更新（Upsert Delta）</b>：使用 UPSERT 将增量数据合并到每日表，避免全量重算</li>
 *   <li><b>容错设计</b>：所有 record* 方法用 try-catch 包裹，指标记录失败不影响主业务</li>
 * </ul>
 *
 * <h3>性能优化</h3>
 * - recordProductView 是高频操作（每次进入商品详情页都调用），
 *   采用异步写入可提升响应速度（后续可引入 RabbitMQ）
 */
@Service
public class ProductMetricServiceImpl implements ProductMetricService {

    private static final Logger log = LoggerFactory.getLogger(ProductMetricServiceImpl.class);

    /** 指标默认统计天数 */
    private static final int DEFAULT_DAYS = 7;

    /** 趋势图默认天数 */
    private static final int DEFAULT_TREND_DAYS = 30;

    private final ProductViewEventMapper viewEventMapper;
    private final ProductMetricDailyMapper metricDailyMapper;

    public ProductMetricServiceImpl(ProductViewEventMapper viewEventMapper,
                                     ProductMetricDailyMapper metricDailyMapper) {
        this.viewEventMapper = viewEventMapper;
        this.metricDailyMapper = metricDailyMapper;
    }

    /**
     * 记录商品浏览事件。
     *
     * <p>容错设计：失败只打警告日志，不影响商品详情页展示。</p>
     *
     * 调用时机：
     * - 用户进入商品详情页时（ProductController.getById）
     * - 从推荐列表点击商品时（source=recommend）
     * - 从搜索结果点击商品时（source=search）
     *
     * @param productId 商品 ID
     * @param userId    用户 ID（可为 null，表示匿名用户）
     * @param source    浏览来源（detail/search/recommend）
     */
    @Override
    public void recordProductView(Long productId, Long userId, String source) {
        if (productId == null) {
            log.warn("recordProductView failed: productId is null");
            return;
        }
        try {
            // 1. 插入浏览事件原始记录（事件溯源：逐条记录，粒度最细）
            ProductViewEvent event = new ProductViewEvent();
            event.setProductId(productId);
            event.setUserId(userId);
            event.setSource(source != null ? source : "detail");
            event.setViewDate(LocalDate.now());
            event.setViewedAt(new Date());
            viewEventMapper.insert(event);

            // 2. 更新每日汇总表（浏览 +1）—— UPSERT 模式：有则累加，无则插入
            metricDailyMapper.upsertDelta(buildMetricDelta(productId, 1, 0, 0, 0, BigDecimal.ZERO));
        } catch (Exception ex) {
            // 容错：指标记录失败不应影响商品详情页的正常展示
            log.warn("Failed to record product view metric for product {}: {}", productId, ex.getMessage());
        }
    }

    /**
     * 记录加购事件。
     *
     * <p>容错设计：失败只打警告日志，不影响加购主流程。</p>
     *
     * 调用时机：CartServiceImpl.addItem() 中，商品成功加入购物车后调用。
     *
     * @param productId 商品 ID
     * @param quantity  加购数量
     */
    @Override
    public void recordCartAddition(Long productId, Integer quantity) {
        if (productId == null || quantity == null || quantity <= 0) {
            return;
        }
        try {
            // 加购计数 +quantity
            metricDailyMapper.upsertDelta(buildMetricDelta(productId, 0, quantity, 0, 0, BigDecimal.ZERO));
        } catch (Exception ex) {
            // 容错：指标记录失败不应影响加购主流程
            log.warn("Failed to record cart metric for product {}: {}", productId, ex.getMessage());
        }
    }

    /**
     * 记录支付成功事件。
     *
     * <p>使用订单项的价格快照（而非当前商品价格）计算金额，保证数据准确性。
     * 使用 @Transactional 保证多个订单项的写入原子性。</p>
     *
     * <p>容错设计：失败只打警告日志，不影响支付主流程。</p>
     *
     * 调用时机：PaymentServiceImpl 中，支付回调成功后调用。
     *
     * @param order      订单实体
     * @param orderItems 订单明细列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordPaymentSuccess(Order order, List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            return;
        }
        try {
            for (OrderItem orderItem : orderItems) {
                if (orderItem == null || orderItem.getProductId() == null
                    || orderItem.getQuantity() == null || orderItem.getQuantity() <= 0) {
                    continue;
                }
                // 计算订单项金额 = 价格快照 × 数量
                BigDecimal lineAmount = safeAmount(orderItem.getPrice())
                    .multiply(BigDecimal.valueOf(orderItem.getQuantity()));

                // 支付订单数 +1，支付数量 +quantity，支付金额 +lineAmount
                metricDailyMapper.upsertDelta(buildMetricDelta(
                    orderItem.getProductId(), 0, 0, 1, orderItem.getQuantity(), lineAmount
                ));
            }
        } catch (Exception ex) {
            // 容错：指标记录失败不应影响支付主流程
            log.warn("Failed to record payment metrics for order {}: {}",
                order == null ? null : order.getOrderNo(), ex.getMessage());
        }
    }

    /**
     * 汇总指定时间窗口的指标。
     *
     * 用途：
     * - 首页展示"累计浏览量"、"累计加购数"等运营数据
     * - 后台管理仪表盘展示整体经营状况
     * - ShowcaseStrategyService 的核心数据源
     *
     * @param days 窗口天数（0 表示使用默认值 7 天）
     * @return 指标汇总（包含 viewCount、cartAddCount、paidOrderCount 等）
     */
    @Override
    public ShowcaseMetricSummary summarizeWindow(int days) {
        LocalDate dateFrom;
        if (days > 0) {
            dateFrom = LocalDate.now().minusDays(days);
        } else {
            // 默认统计最近 7 天
            dateFrom = LocalDate.now().minusDays(DEFAULT_DAYS);
        }

        ShowcaseMetricSummary summary = metricDailyMapper.aggregateSummary(dateFrom);
        if (summary == null) {
            // 如果没有数据，返回空对象（避免 NPE）
            summary = new ShowcaseMetricSummary();
        }

        log.debug("Metric summary query: days={}, viewCount={}, cartAddCount={}",
            days, summary.getViewCount(), summary.getCartAddCount());

        return summary;
    }

    /**
     * 获取最近 N 天的每日指标明细。
     *
     * 用途：
     * - 首页展示趋势图（折线图/柱状图）
     * - 对比不同时间段的经营状况
     *
     * @param limit 天数限制（0 表示使用默认值 30 天）
     * @return 每日指标列表（按日期倒序）
     */
    @Override
    public List<ShowcaseDailyMetricItem> recentDailyMetrics(int limit) {
        int days = limit > 0 ? limit : DEFAULT_TREND_DAYS;

        List<ShowcaseDailyMetricItem> items = metricDailyMapper.selectRecentDailyTotals(days);
        if (items == null) {
            return Collections.emptyList();
        }

        log.debug("Daily metrics query: days={}, returned={} records", days, items.size());

        return items;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建每日指标增量对象。
     *
     * 用于 upsertDelta 方法：将各维度的增量值封装为 ProductMetricDaily 对象，
     * 由 Mapper XML 中的 ON DUPLICATE KEY UPDATE 实现累加。
     *
     * @param productId       商品 ID
     * @param viewCount       浏览增量
     * @param cartAddCount    加购增量
     * @param paidOrderCount  支付订单增量
     * @param paidQuantity    支付数量增量
     * @param paidAmount      支付金额增量
     * @return 封装好增量值的 ProductMetricDaily 对象
     */
    private ProductMetricDaily buildMetricDelta(Long productId,
                                                int viewCount,
                                                int cartAddCount,
                                                int paidOrderCount,
                                                int paidQuantity,
                                                BigDecimal paidAmount) {
        ProductMetricDaily metric = new ProductMetricDaily();
        metric.setMetricDate(LocalDate.now());
        metric.setProductId(productId);
        metric.setViewCount(viewCount);
        metric.setCartAddCount(cartAddCount);
        metric.setPaidOrderCount(paidOrderCount);
        metric.setPaidQuantity(paidQuantity);
        metric.setPaidAmount(safeAmount(paidAmount));
        return metric;
    }

    /** 安全的 BigDecimal 空值处理（null -> ZERO） */
    private BigDecimal safeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
