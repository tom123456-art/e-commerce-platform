package com.example.ecommerce.dto;

import com.example.ecommerce.config.ShowcaseProperties;
import lombok.Data;

/**
 * 【教学：请求DTO】商品展陈策略配置请求。
 *
 * <h3>本类的业务场景</h3>
 * <p>管理员在后台调整商品展陈（Showcase）算法的参数配置。
 * 展陈策略决定了首页"猜你喜欢"、"热销排行"等模块展示哪些商品、如何排序。</p>
 *
 * <h3>请求 DTO 中的"配置对象"模式</h3>
 * <p>本类的字段类型包含嵌套的配置对象（HotWeights、AnonymousWeights 等），
 * 这是一种常见的配置管理设计：</p>
 * <ul>
 *   <li><b>扁平 DTO</b>：每个配置项一个字段，适合简单场景</li>
 *   <li><b>嵌套配置对象</b>：相关配置分组到子对象中，适合复杂场景</li>
 * </ul>
 * <p>本类采用嵌套模式，将不同推荐策略的权重配置分别管理，
 * 保持了代码的结构清晰和可维护性。</p>
 *
 * <h3>策略模式的参数说明</h3>
 * <p>展陈系统支持多种推荐模式，不同模式关注不同的信号：</p>
 * <ul>
 *   <li><b>hot（热销模式）</b>：基于销量、浏览量等热度信号推荐</li>
 *   <li><b>anonymous（匿名模式）</b>：对未登录用户，基于全局行为数据推荐</li>
 *   <li><b>personalized（个性化模式）</b>：对已登录用户，基于个人行为推荐</li>
 * </ul>
 *
 * @see ShowcaseStrategyResponse 对应的策略配置响应DTO
 * @see ShowcaseProperties 展陈配置的属性定义类
 */
@Data
public class ShowcaseStrategyRequest {

    /**
     * 推荐模式。
     *
     * <p>可选值包括 "hot"、"anonymous"、"personalized" 等。
     * 不同模式下，其他参数的含义和权重配置会有所不同。</p>
     */
    private String mode;

    /**
     * 短期窗口天数。
     *
     * <p>用于计算近期热度信号的时间范围。例如设为 7，表示只看最近7天的数据。
     * 短窗口对突发趋势（如爆款商品）更敏感。</p>
     */
    private Integer shortWindowDays;

    /**
     * 长期窗口天数。
     *
     * <p>用于计算长期趋势信号的时间范围。例如设为 30，表示看最近30天的数据。
     * 长窗口更稳定，适合识别持久热销商品。</p>
     */
    private Integer longWindowDays;

    /**
     * 购物车偏好权重。
     *
     * <p>在个性化推荐中，用户加购过某类商品的权重系数。
     * 值越大，推荐结果越偏向用户曾经加购过的商品类别。</p>
     */
    private Double cartPreferenceWeight;

    /**
     * 热销模式的权重配置。
     *
     * <p>定义在热销模式下，各项信号（销量、浏览量、加购量等）的相对重要性。
     * 类型为 {@link ShowcaseProperties.HotWeights}，是外部配置类的嵌套子类。</p>
     */
    private ShowcaseProperties.HotWeights hot;

    /**
     * 匿名用户模式的权重配置。
     *
     * <p>针对未登录用户的推荐策略权重。匿名用户没有个人历史数据，
     * 只能基于全局行为数据（如"最多人浏览"）进行推荐。</p>
     */
    private ShowcaseProperties.AnonymousWeights anonymous;

    /**
     * 个性化模式的权重配置。
     *
     * <p>针对已登录用户的推荐策略权重。可以利用用户的浏览历史、
     * 购买记录、加购记录等个人数据进行精准推荐。</p>
     */
    private ShowcaseProperties.PersonalizedWeights personalized;

    /**
     * 热度信号的权重配置。
     *
     * <p>定义各项热度信号（如浏览量、加购量、成交量）的权重比例，
     * 用于综合计算商品的热度评分。</p>
     */
    private ShowcaseProperties.HotSignalWeights hotSignal;
}
