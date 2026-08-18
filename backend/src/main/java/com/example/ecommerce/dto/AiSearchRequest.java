package com.example.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 【教学：请求DTO】AI 智能搜索请求的数据传输对象。
 *
 * <h3>本类与 ProductQueryRequest 的对比</h3>
 * <p>本项目中存在两种搜索方式，体现了传统搜索与 AI 搜索的差异：</p>
 * <table border="1">
 *   <tr><th>维度</th><th>ProductQueryRequest（传统搜索）</th><th>AiSearchRequest（AI搜索）</th></tr>
 *   <tr><td>输入方式</td><td>关键词 + 多个筛选条件（分类、价格区间、排序）</td><td>一句自然语言描述</td></tr>
 *   <tr><td>搜索原理</td><td>SQL LIKE / 精确匹配</td><td>AI 语义理解 + 向量相似度</td></tr>
 *   <tr><td>用户体验</td><td>需要用户理解筛选条件</td><td>像和人对话一样自然</td></tr>
 *   <tr><td>典型输入</td><td>keyword="耳机", category=1, minPrice=100</td><td>"降噪效果好的无线耳机，预算500以内"</td></tr>
 * </table>
 *
 * <h3>为什么只有一个字段？</h3>
 * <p>AI 搜索的精髓在于：<b>把复杂的筛选逻辑交给 AI 去理解</b>。
 * 用户不需要知道"分类ID"或"价格区间"怎么填，只需要用自然语言描述需求。
 * AI 会自动从一句话中提取意图、关键词、价格范围、分类偏好等信息。</p>
 * <p>这是 DTO 设计中"简化接口"思想的体现：对外接口越简单，调用方的负担越小。</p>
 *
 * @see AiSearchResponse 对应的AI搜索响应DTO
 */
@Data
public class AiSearchRequest {

    /**
     * 用户的搜索内容（自然语言）。
     *
     * <p><b>验证注解：</b>{@code @NotBlank} 确保搜索内容非空。</p>
     *
     * <p><b>与传统搜索的 keyword 的区别：</b></p>
     * <ul>
     *   <li>传统 keyword："耳机" — 精确匹配商品名称中的关键词</li>
     *   <li>AI query："我想买一个跑步时戴的不容易掉的耳机" —
     *       AI 理解场景（跑步）、需求（不容易掉），推荐运动耳机</li>
     * </ul>
     */
    @NotBlank(message = "搜索内容不能为空")
    private String query;
}
