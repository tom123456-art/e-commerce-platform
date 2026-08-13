package com.example.ecommerce.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * 商品展示策略配置属性 —— 通过 application.yml 配置推荐算法权重。
 *
 * <h3>@ConfigurationProperties 嵌套对象支持</h3>
 * Spring Boot 自动创建嵌套对象并绑定配置值：
 * <pre>
 *   ecommerce.showcase.hot.sales → HotWeights.sales
 *   ecommerce.showcase.anonymous.hot → AnonymousWeights.hot
 *   ecommerce.showcase.personalized.category → PersonalizedWeights.category
 * </pre>
 *
 * <h3>权重配置的意义</h3>
 * 每个维度有权重值，所有权重组内和应为 1.0。
 * 最终得分 = 维度1得分 × 权重1 + 维度2得分 × 权重2 + ...
 */
@Data
@Component
@ConfigurationProperties(prefix = "ecommerce.showcase")
public class ShowcaseProperties {

    /** 热销商品权重配置 */
    private HotWeights hot = new HotWeights();

    /** 匿名用户（未登录）展示权重配置 */
    private AnonymousWeights anonymous = new AnonymousWeights();

    /** 个性化推荐权重配置 */
    private PersonalizedWeights personalized = new PersonalizedWeights();

    /** 热销信号权重配置 */
    private HotSignalWeights hotSignal = new HotSignalWeights();

    /**
     * 购物车偏好权重。
     * 配置项：ecommerce.showcase.cart-preference-weight
     */
    private double cartPreferenceWeight = 0.60D;

    /**
     * 热销商品权重配置。
     * <p>配置项前缀：ecommerce.showcase.hot</p>
     * <p>热度得分 = 销量×sales + 收入×revenue + 订单数×orders + 新鲜度×freshness + 库存×inventory</p>
     */
    @Data
    public static class HotWeights {
        /** 销量权重（默认 55%）—— 销量是热度最核心指标 */
        private double sales = 0.55D;
        /** 收入权重（默认 15%）—— 高客单价商品热度加成 */
        private double revenue = 0.15D;
        /** 订单数权重（默认 15%）—— 与销量类似但去除了数量因素 */
        private double orders = 0.15D;
        /** 新鲜度权重（默认 10%）—— 新上架商品热度加成 */
        private double freshness = 0.10D;
        /** 库存权重（默认 5%）—— 库存充足商品优先展示 */
        private double inventory = 0.05D;
    }

    /**
     * 匿名用户展示权重配置。
     * <p>配置项前缀：ecommerce.showcase.anonymous</p>
     */
    @Data
    public static class AnonymousWeights {
        private double hot = 0.50D;            // 热销权重（默认 50%）
        private double freshness = 0.25D;      // 新鲜度（默认 25%）
        private double inventory = 0.15D;      // 库存（默认 15%）
        private double affordability = 0.10D;  // 性价比（默认 10%）
    }

    /**
     * 个性化推荐权重配置。
     * <p>配置项前缀：ecommerce.showcase.personalized</p>
     */
    @Data
    public static class PersonalizedWeights {
        private double category = 0.50D;       // 类别偏好（默认 50%）—— 个性化推荐核心
        private double hot = 0.25D;             // 热销（默认 25%）
        private double price = 0.10D;           // 价格区间（默认 10%）
        private double freshness = 0.10D;       // 新鲜度（默认 10%）
        private double inventory = 0.05D;       // 库存（默认 5%）
    }

    /**
     * 热销信号权重配置（实时信号，区别于 HotWeights 的历史热度）。
     * <p>配置项前缀：ecommerce.showcase.hot-signal</p>
     */
    @Data
    public static class HotSignalWeights {
        private double sales = 0.50D;
        private double revenue = 0.20D;
        private double orders = 0.20D;
        private double freshness = 0.10D;
    }
}
