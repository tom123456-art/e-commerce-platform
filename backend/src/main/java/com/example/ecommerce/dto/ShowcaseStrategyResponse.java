package com.example.ecommerce.dto;

import com.example.ecommerce.config.ShowcaseProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 【教学：响应DTO】商品展陈策略配置响应。
 *
 * <h3>响应 DTO 扩展请求 DTO 的模式</h3>
 * <p>本类包含了 {@link ShowcaseStrategyRequest} 中的所有配置字段，
 * 并额外增加了以下信息：</p>
 * <ul>
 *   <li><b>lastAutoTunedAt</b>：上次自动调优时间 —— 系统定时自动调整策略的时间戳</li>
 *   <li><b>updateTime</b>：上次手动更新时间 —— 管理员手动修改策略的时间戳</li>
 *   <li><b>shortWindowSummary / longWindowSummary</b>：当前策略的运行效果指标</li>
 *   <li><b>recentDailyMetrics</b>：最近的每日指标明细数据</li>
 * </ul>
 *
 * <p>这是响应 DTO 的典型设计模式：<b>回显请求参数 + 补充后端计算结果</b>。
 * 管理员不仅能看"当前配置是什么"，还能看"当前配置效果如何"。</p>
 *
 * <h3>为什么配置类字段用 new 初始化？</h3>
 * <p>注意 {@code private ShowcaseProperties.HotWeights hot = new ShowcaseProperties.HotWeights();}
 * 这种写法，它保证字段永远不为 null。在序列化为 JSON 时：</p>
 * <ul>
 *   <li>null 字段 → JSON 中该字段被省略（取决于 Jackson 配置）</li>
 *   <li>空对象 → JSON 中显示 {@code "hot": {...}}，前端可以直接读取子字段</li>
 * </ul>
 * <p>使用空对象而非 null，可以避免前端的 NullPointerException（JavaScript 中的 TypeError）。</p>
 *
 * @see ShowcaseStrategyRequest 对应的策略配置请求DTO
 * @see ShowcaseMetricSummary 包含的指标汇总DTO
 * @see ShowcaseDailyMetricItem 包含的每日指标明细DTO
 */
@Data
public class ShowcaseStrategyResponse {

    /**
     * 当前推荐模式。
     */
    private String mode;

    /**
     * 短期窗口天数。
     */
    private Integer shortWindowDays;

    /**
     * 长期窗口天数。
     */
    private Integer longWindowDays;

    /**
     * 购物车偏好权重。
     */
    private Double cartPreferenceWeight;

    /**
     * 热销模式的权重配置（默认空对象，避免 null）。
     */
    private ShowcaseProperties.HotWeights hot = new ShowcaseProperties.HotWeights();

    /**
     * 匿名用户模式的权重配置（默认空对象，避免 null）。
     */
    private ShowcaseProperties.AnonymousWeights anonymous = new ShowcaseProperties.AnonymousWeights();

    /**
     * 个性化模式的权重配置（默认空对象，避免 null）。
     */
    private ShowcaseProperties.PersonalizedWeights personalized = new ShowcaseProperties.PersonalizedWeights();

    /**
     * 热度信号的权重配置（默认空对象，避免 null）。
     */
    private ShowcaseProperties.HotSignalWeights hotSignal = new ShowcaseProperties.HotSignalWeights();

    /**
     * 上次系统自动调优的时间。
     *
     * <p>展陈系统支持定时自动优化策略参数。此字段记录最近一次自动调优的时间，
     * 便于管理员了解策略的"新鲜度"。</p>
     */
    private Date lastAutoTunedAt;

    /**
     * 上次手动更新的时间。
     *
     * <p>管理员通过后台页面手动修改策略时记录的时间戳。
     * 与 lastAutoTunedAt 配合，可以区分"系统自动调整"和"人工手动调整"。</p>
     */
    private Date updateTime;

    /**
     * 短期窗口的指标汇总。
     *
     * <p>展示最近 shortWindowDays 天的运营指标汇总，帮助管理员
     * 评估当前策略在近期的表现。</p>
     *
     * @see ShowcaseMetricSummary 汇总指标DTO
     */
    private ShowcaseMetricSummary shortWindowSummary;

    /**
     * 长期窗口的指标汇总。
     *
     * <p>展示最近 longWindowDays 天的运营指标汇总，用于观察长期趋势。</p>
     */
    private ShowcaseMetricSummary longWindowSummary;

    /**
     * 最近的每日指标明细列表。
     *
     * <p>用于前端绘制趋势折线图。默认为空列表（非 null），
     * 避免前端遍历时出现 TypeError。</p>
     */
    private List<ShowcaseDailyMetricItem> recentDailyMetrics = new ArrayList<ShowcaseDailyMetricItem>();
}
