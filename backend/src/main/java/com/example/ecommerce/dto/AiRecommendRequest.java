package com.example.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 【教学：请求DTO】AI 商品推荐请求的数据传输对象。
 *
 * <h3>本类的业务场景</h3>
 * <p>用户在首页或搜索页输入自然语言需求（如"适合送女朋友的礼物"），
 * AI 理解意图后从商品库中筛选并推荐最匹配的商品。</p>
 *
 * <h3>请求 DTO 中的"渐进式细化"设计</h3>
 * <p>本类展示了如何通过可选参数逐步缩小推荐范围：</p>
 * <ol>
 *   <li><b>query（必填）</b>：自然语言需求描述，是 AI 理解用户意图的唯一依据</li>
 *   <li><b>budget（可选）</b>：价格上限，帮助 AI 过滤超出预算的商品</li>
 *   <li><b>categoryPreference（可选）</b>：偏好分类，进一步聚焦推荐范围</li>
 * </ol>
 * <p>前端可以只传 query（最简模式），也可以传全部参数（精准模式），
 * 后端和 AI 会根据可用信息灵活调整推荐策略。</p>
 *
 * <h3>金额字段为什么用 BigDecimal？</h3>
 * <p>Java 中 {@code double} 和 {@code float} 存在精度问题：
 * {@code 0.1 + 0.2 != 0.3}（实际是 0.30000000000000004）。
 * 金融/电商场景中，金额计算必须使用 {@code BigDecimal} 来保证精度。
 * 这是 Java 企业级开发的基本规范。</p>
 *
 * @see AiRecommendResponse 对应的AI推荐响应DTO
 */
@Data
public class AiRecommendRequest {

    /**
     * 用户的自然语言需求描述。
     *
     * <p><b>验证规则：</b>{@code @NotBlank} 确保需求描述不为空。
     * 没有需求描述，AI 无法理解用户意图，推荐也就无从谈起。</p>
     *
     * <p><b>示例输入：</b></p>
     * <ul>
     *   <li>"适合送女朋友的生日礼物"</li>
     *   <li>"性价比高的蓝牙耳机"</li>
     *   <li>"适合户外运动的装备"</li>
     * </ul>
     */
    @NotBlank(message = "需求描述不能为空")
    private String query;

    /**
     * 预算上限（可选）。
     *
     * <p>使用 {@code BigDecimal} 表示金额，避免浮点数精度问题。
     * 当用户设置了预算，AI 会排除价格超过此值的商品。</p>
     *
     * <p><b>可选字段不加验证注解：</b>null 表示用户未设预算限制，
     * AI 将推荐各价位的优质商品。</p>
     */
    private BigDecimal budget;

    /**
     * 分类偏好（可选）。
     *
     * <p>用户希望推荐的商品分类，例如"电子产品"、"服饰"等。
     * 提供此参数可以让推荐更聚焦，减少无关结果。</p>
     */
    private String categoryPreference;
}
