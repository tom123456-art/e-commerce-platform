package com.example.ecommerce.dto;

/**
 * 仪表盘图表数据项（通用）。
 *
 * <h3>为什么设计为通用类？</h3>
 * 仪表盘有多种图表（订单状态分布、商品分类分布、用户角色分布...），
 * 它们的数据结构完全相同（都是"名称 + 数量"的键值对）。
 * 通用 DTO 的优势：
 * <ul>
 *   <li><b>减少类数量</b>：一个类搞定所有饼图/柱状图数据</li>
 *   <li><b>前端统一处理</b>：一个图表组件渲染所有分布图</li>
 *   <li><b>易于扩展</b>：新增图表无需新增 DTO 类</li>
 * </ul>
 *
 * <h3>前端 ECharts 配合示例</h3>
 * <pre>{@code
 * const chartData = response.orderStatusDistribution; // DashboardChartItem[]
 * option = {
 *   series: [{
 *     type: 'pie',
 *     data: chartData.map(item => ({ name: item.label, value: item.value }))
 *   }]
 * };
 * }</pre>
 */
public class DashboardChartItem {

    /**
     * 数据项标签。
     * 饼图中作为扇区名称，柱状图中作为 X 轴标签。
     * 如："待支付"、"电子产品"、"管理员"。
     */
    private String label;

    /**
     * 数据项数值。
     * <p>使用 Double 而非 BigDecimal：
     * 图表数据不需要精确到分位，Double 精度够用。
     * 且前端 JS 图表库直接用 number 类型，Double 序列化后就是 JSON number，无需转换。</p>
     */
    private Double value;

    /**
     * 无参构造（JSON 反序列化需要）
     */
    public DashboardChartItem() {
    }

    /**
     * 全参构造（方便 Service 层构造）
     */
    public DashboardChartItem(String label, Double value) {
        this.label = label;
        this.value = value;
    }

    // Getter / Setter 省略...
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }
}
