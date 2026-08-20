package com.example.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 【教学：响应DTO】AI 商品文案生成响应的数据传输对象。
 *
 * <h3>Lombok 组合注解详解</h3>
 * <p>本类使用了多个 Lombok 注解，展示了 Lombok 的灵活组合能力：</p>
 * <ul>
 *   <li>{@code @Data} — 生成 getter/setter/toString/equals/hashCode</li>
 *   <li>{@code @Builder} — 生成建造者模式代码，支持链式构造：
 *       <pre>
 *       AiDescribeResponse.builder()
 *           .description("这是一款...")
 *           .seoTitle("XX产品 - 品质之选")
 *           .highlights(List.of("卖点1", "卖点2"))
 *           .build();
 *       </pre>
 *   </li>
 *   <li>{@code @NoArgsConstructor} — 生成无参构造方法（JSON 反序列化需要）</li>
 *   <li>{@code @AllArgsConstructor} — 生成全参构造方法</li>
 * </ul>
 *
 * <p><b>为什么需要同时有 @Builder 和 @NoArgsConstructor？</b></p>
 * <p>Spring 的 Jackson 反序列化器（将 JSON 映射为 Java 对象）默认需要无参构造方法。
 *
 * @Builder 会生成一个全参构造方法，反而覆盖了默认的无参构造，导致反序列化失败。
 * 因此需要显式加上 @NoArgsConstructor 来补回无参构造。</p>
 * @see AiDescribeRequest 对应的AI文案生成请求DTO
 */
@lombok.Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiDescribeResponse {

    /**
     * AI 生成的商品描述文案。
     *
     * <p>通常是一段完整的文字，适合直接用于商品详情页的描述区域。
     * 内容会根据请求中的 category、keyFeatures、style 等参数进行定制。</p>
     */
    private String description;

    /**
     * AI 生成的 SEO 优化标题。
     *
     * <p>SEO（Search Engine Optimization，搜索引擎优化）标题需要：</p>
     * <ul>
     *   <li>包含核心关键词，提升搜索引擎排名</li>
     *   <li>长度适中（通常 15-30 个中文字符）</li>
     *   <li>吸引用户点击</li>
     * </ul>
     * <p>例如：{@code "2024新款轻薄笔记本电脑 | 16英寸大屏 高性能办公本"}</p>
     */
    private String seoTitle;

    /**
     * AI 提取/生成的商品卖点列表。
     *
     * <p>每个字符串代表一个独立的卖点，前端通常以列表或标签形式展示。
     * 例如：</p>
     * <ul>
     *   <li>"航空级铝合金机身，轻至 1.2kg"</li>
     *   <li>"16英寸 2K 高清屏幕"</li>
     *   <li>"12小时超长续航"</li>
     * </ul>
     */
    private List<String> highlights;
}
