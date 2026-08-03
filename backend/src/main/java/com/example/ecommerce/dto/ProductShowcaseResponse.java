package com.example.ecommerce.dto;

import com.example.ecommerce.entity.Product;

/**
 * 【教学：响应DTO】商品展陈推荐响应（带推荐元数据）。
 *
 * <h3>本类的业务场景</h3>
 * <p>首页的"猜你喜欢"、"热销推荐"等展陈模块，需要返回商品信息以及推荐相关的元数据。
 * 本类将 Product Entity 与推荐算法的输出（评分、标签、理由）组合在一起。</p>
 *
 * <h3>Entity + 元数据 的 DTO 组合模式</h3>
 * <p>本类展示了一种常见的 DTO 设计模式：</p>
 * <pre>
 * ProductShowcaseResponse
 * ├── product: Product      ← 来自数据库的实体数据
 * ├── score: Double          ← 算法计算的推荐评分
 * ├── tag: String            ← 算法生成的标签（如"热销"、"新品"）
 * └── reason: String         ← 算法生成的推荐理由
 * </pre>
 * <p>Product Entity 是"通用的"，适用于各种场景；而 score/tag/reason 是"场景特定的"，
 * 只在展陈推荐场景下才有意义。DTO 的灵活性允许我们将两者组合。</p>
 *
 * <h3>为什么不修改 Entity？</h3>
 * <p>如果在 Product Entity 中加 score、tag、reason 字段，会导致：</p>
 * <ul>
 *   <li><b>职责混乱</b>：Entity 应该只映射数据库字段，不应包含算法输出</li>
 *   <li><b>字段污染</b>：非推荐场景下这些字段永远为 null</li>
 *   <li><b>耦合加剧</b>：推荐算法的变化会直接影响数据模型</li>
 * </ul>
 * <p>使用 DTO 组合模式，Entity 保持纯净，推荐元数据只在需要时出现。</p>
 *
 * @see com.example.ecommerce.entity.Product 商品实体
 * @see ShowcaseStrategyResponse 展陈策略配置响应
 */
public class ProductShowcaseResponse {

    /**
     * 商品实体信息。
     *
     * <p>包含商品的完整数据库字段：id、name、price、description、image、
     * stock、category、createTime 等。前端直接从中获取展示所需的所有数据。</p>
     */
    private Product product;

    /**
     * 推荐评分（0.0 ~ 100.0）。
     *
     * <p>由展陈算法根据多种信号（热度、个性化匹配度、转化率等）综合计算。
     * 分数越高，表示该商品越适合推荐给当前用户。</p>
     *
     * <p>前端可能用此值调整展示顺序，或显示"推荐指数 95 分"等信息。</p>
     */
    private Double score;

    /**
     * 推荐标签。
     *
     * <p>展陈算法为商品打的标签，例如：</p>
     * <ul>
     *   <li>"热销" — 近期销量最高的商品</li>
     *   <li>"新品" — 最近上架的商品</li>
     *   <li>"好评如潮" — 用户评分最高的商品</li>
     *   <li>"性价比之王" — 同类商品中价格最优的</li>
     * </ul>
     * <p>前端通常以彩色标签或徽章形式展示在商品卡片上。</p>
     */
    private String tag;

    /**
     * 推荐理由（面向用户）。
     *
     * <p>一段简短的自然语言说明，解释为什么推荐这个商品。
     * 例如："最近7天被 328 人浏览"、"与您浏览过的 XX 商品相似"。</p>
     *
     * <p>前端展示此信息可以增强用户的信任感和购买意愿。</p>
     */
    private String reason;

    /**
     * 无参构造方法，供 JSON 反序列化使用。
     */
    public ProductShowcaseResponse() {
    }

    /**
     * 全参构造方法，方便 Service 层快速构建响应。
     *
     * @param product 商品实体
     * @param score   推荐评分
     * @param tag     推荐标签
     * @param reason  推荐理由
     */
    public ProductShowcaseResponse(Product product, Double score, String tag, String reason) {
        this.product = product;
        this.score = score;
        this.tag = tag;
        this.reason = reason;
    }

    // ========== Getter / Setter ==========

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
