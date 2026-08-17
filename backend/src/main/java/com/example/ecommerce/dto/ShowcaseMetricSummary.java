package com.example.ecommerce.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 【教学：响应DTO】商品展陈指标汇总数据。
 *
 * <h3>本类与 ShowcaseDailyMetricItem 的关系</h3>
 * <p>两个类形成"明细 + 汇总"的数据组合：</p>
 * <ul>
 *   <li><b>ShowcaseDailyMetricItem</b>：按天拆分的明细数据，用于绘制趋势折线图</li>
 *   <li><b>ShowcaseMetricSummary</b>：指定时间窗口内的汇总数据，用于展示概览卡片</li>
 * </ul>
 *
 * <h3>时间窗口（Window）概念</h3>
 * <p>电商运营通常对比不同时间窗口的数据来判断趋势：</p>
 * <ul>
 *   <li><b>短期窗口</b>（如 7 天）：反映近期变化，对突发波动敏感</li>
 *   <li><b>长期窗口</b>（如 30 天）：反映整体趋势，平滑短期波动</li>
 * </ul>
 * <p>本类通过 {@code windowDays} 字段标识汇总的时间范围，同一个类可以用于
 * 7天汇总和30天汇总，体现了 DTO 的复用性。</p>
 *
 * <h3>转化率指标的层次</h3>
 * <pre>
 * 浏览用户 ──viewToCartRate──→ 加购用户 ──cartPaymentRate──→ 付费用户
 * └──────────────── paymentRate ──────────────────────────────┘
 * </pre>
 * <p>三个转化率从不同角度衡量展陈效果，帮助管理员定位漏斗中的薄弱环节。</p>
 *
 * @see ShowcaseDailyMetricItem 与本类配合使用的每日明细DTO
 */
@Data
public class ShowcaseMetricSummary {

    /**
     * 汇总的时间窗口天数。
     *
     * <p>例如 7 表示"最近7天的数据汇总"，30 表示"最近30天"。
     * 此字段由后端根据请求参数设置，前端据此展示"过去7天概览"等标题。</p>
     */
    private Integer windowDays;

    /**
     * 时间窗口内的总浏览次数。
     */
    private Long viewCount = 0L;

    /**
     * 时间窗口内的总加购次数。
     */
    private Long cartAddCount = 0L;

    /**
     * 时间窗口内的已支付订单数。
     */
    private Long paidOrderCount = 0L;

    /**
     * 时间窗口内的已支付商品总数量。
     */
    private Long paidQuantity = 0L;

    /**
     * 时间窗口内的已支付总金额。
     */
    private BigDecimal paidAmount = BigDecimal.ZERO;

    /**
     * 浏览→加购 转化率。
     *
     * <p>计算公式：cartAddCount / viewCount。
     * 此指标反映商品展示对用户的吸引力——用户看到了，是否产生购买欲望。</p>
     */
    private Double viewToCartRate = 0D;

    /**
     * 浏览→支付 转化率（整体转化率）。
     *
     * <p>计算公式：paidOrderCount / viewCount。
     * 从浏览到最终购买的全链路转化率，是衡量展陈效果的最核心指标。</p>
     */
    private Double paymentRate = 0D;

    /**
     * 加购→支付 转化率。
     *
     * <p>计算公式：paidOrderCount / cartAddCount。
     * 此指标反映"临门一脚"的转化能力——用户加购了，是否最终付款。
     * 如果此指标偏低，可能需要优化结算流程或价格策略。</p>
     */
    private Double cartPaymentRate = 0D;
}
