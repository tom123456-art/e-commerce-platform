package com.example.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 【教学：请求DTO】AI 商品文案生成请求的数据传输对象。
 *
 * <h3>本类的业务场景</h3>
 * <p>商家在发布商品时，可以借助 AI 自动生成商品描述文案。
 * 前端将商品的基本信息（名称、分类、卖点、风格偏好）发送给后端，
 * 后端调用 AI 大语言模型生成专业的商品描述、SEO 标题和卖点列表。</p>
 *
 * <h3>请求 DTO 的"分层验证"策略</h3>
 * <p>本类展示了请求 DTO 中"必填 + 可选"字段的典型组合：</p>
 * <ul>
 *   <li><b>productName</b>：用 {@code @NotBlank} 标记为必填 —— 没有商品名称，
 *       AI 无法生成有意义的文案</li>
 *   <li><b>category / keyFeatures / style</b>：不加验证注解，标记为可选 ——
 *       这些信息越详细，AI 生成的文案越精准，但缺失时 AI 可以使用通用模板</li>
 * </ul>
 * <p>这种"核心必填、细节可选"的设计，在前端体验和数据质量之间取得平衡。</p>
 *
 * <h3>对比 Entity 思考</h3>
 * <p>如果用 Entity 传输，前端必须传入数据库中的商品完整字段（id、价格、库存、
 * 创建时间...），但文案生成只关心名称和卖点。DTO 让前端只传必要字段，
 * 降低了前后端的耦合度。</p>
 *
 * @see AiDescribeResponse 对应的AI文案生成响应DTO
 */
@Data
public class AiDescribeRequest {

    /**
     * 商品名称，文案生成的核心输入。
     *
     * <p><b>验证注解说明：</b></p>
     * <ul>
     *   <li>{@code @NotBlank(message = "商品名称不能为空")}
     *       <ul>
     *         <li>验证时机：Spring 在反序列化 JSON 后、进入 Controller 方法前自动触发</li>
     *         <li>失败行为：抛出 {@code MethodArgumentNotValidException}，
     *             由全局异常处理器统一返回 400 错误和 message 信息</li>
     *         <li>message 属性：定义验证失败时返回给前端的中文提示文案</li>
     *       </ul>
     *   </li>
     * </ul>
     */
    @NotBlank(message = "商品名称不能为空")
    private String productName;

    /**
     * 商品分类（可选）。
     *
     * <p>例如："电子产品"、"服装"、"食品" 等。AI 会根据分类调整文案的语气和专业术语。
     * 例如，电子产品文案侧重参数和性能，食品文案侧重口感和食材。</p>
     *
     * <p><b>不加 @NotBlank 的原因：</b>分类信息缺失时，AI 仍然可以根据商品名称
     * 生成通用文案，不会导致业务异常。</p>
     */
    private String category;

    /**
     * 商品关键卖点/特征（可选）。
     *
     * <p>商家用自然语言描述商品的核心卖点，例如：</p>
     * <ul>
     *   <li>"采用航空级铝合金外壳，重量仅180g"</li>
     *   <li>"有机种植，零农药残留"</li>
     * </ul>
     * <p>这些信息会被 AI 整合进生成的描述文案中，提升文案的针对性和说服力。</p>
     */
    private String keyFeatures;

    /**
     * 文案风格偏好（可选）。
     *
     * <p>允许商家指定文案的写作风格，例如：</p>
     * <ul>
     *   <li>"专业严谨" — 适合 B2B 或技术类产品</li>
     *   <li>"活泼有趣" — 适合年轻消费群体</li>
     *   <li>"高端奢华" — 适合奢侈品牌</li>
     * </ul>
     * <p>未指定时，AI 会根据商品分类自动选择合适的风格。</p>
     */
    private String style;
}
