package com.example.ecommerce.service;

import com.example.ecommerce.dto.ShowcaseStrategyRequest;
import com.example.ecommerce.dto.ShowcaseStrategyResponse;

/**
 * 商品展示策略服务接口 —— 定义商品展示权重的管理、手动配置和自动调优功能。
 *
 * <h3>【教学】Service 层在分层架构中的作用</h3>
 * <p>
 * 展示策略服务是电商系统中"智能运营"的核心，它体现了 Service 层的"业务规则封装"能力：
 * </p>
 * <ul>
 *   <li><b>复杂的业务规则</b>：展示策略涉及多维权重（热门、匿名、个性化、热度信号）
 *       的计算、归一化、混合（blend）等数学运算，这些逻辑完全封装在 Service 实现层，
 *       Controller 只需调用简单的方法即可。</li>
 *   <li><b>配置持久化</b>：策略配置需要持久化到数据库（而非仅存内存），
 *       Service 层负责"内存配置 ↔ 数据库配置"的双向同步。</li>
 *   <li><b>缓存一致性</b>：策略变更后需要清除 Redis 中的商品展示缓存，
 *       确保下次请求使用新策略。</li>
 * </ul>
 *
 * <h3>【教学】面向接口编程的设计原则</h3>
 * <p>
 * 本接口的四个方法覆盖了策略管理的完整生命周期：
 * </p>
 * <ul>
 *   <li>{@code getStrategy} —— 读取当前策略（查询）</li>
 *   <li>{@code saveStrategy} —— 保存策略配置（更新）</li>
 *   <li>{@code autoTuneNow} —— 立即执行自动调优（主动触发）</li>
 *   <li>{@code autoTuneIfEnabled} —— 按需自动调优（定时任务触发）</li>
 * </ul>
 * <p>
 * "读取"和"保存"是标准的 CRUD 操作，"自动调优"则是本系统的特色功能。
 * 将"自动调优"也定义在接口中，使得调用方（如定时任务 ScheduledTaskService）
 * 只需依赖接口，不关心调优的具体算法实现。
 * </p>
 *
 * <h3>【教学】本接口的业务定位</h3>
 * <p>
 * 商品展示策略决定了"首页展示哪些商品、以什么顺序展示"。系统支持两种模式：
 * </p>
 * <ul>
 *   <li><b>MANUAL（手动模式）</b>：管理员手动配置各维度的权重比例。
 *       适用于运营团队有明确策略方向的场景（如"双 11 期间重点推销量"）。</li>
 *   <li><b>AUTO（自动模式）</b>：系统根据商品指标（浏览量、加购率、支付转化率等）
 *       自动计算并调整权重。使用"短期窗口 vs 长期窗口"的动量分析方法，
 *       识别业务趋势并动态调整策略。例如发现最近 7 天销量增长明显，
 *       则自动提高"销量"维度的权重。</li>
 * </ul>
 * <p>
 * 权重体系包括四个维度：
 * </p>
 * <ul>
 *   <li><b>HotWeights</b>：热门商品权重（销量、收入、订单数、新鲜度、库存）</li>
 *   <li><b>AnonymousWeights</b>：匿名用户权重（热度、新鲜度、库存、性价比）</li>
 *   <li><b>PersonalizedWeights</b>：个性化推荐权重（分类、热度、价格、新鲜度、库存）</li>
 *   <li><b>HotSignalWeights</b>：热度信号权重（销量信号、收入信号、订单信号、新鲜度信号）</li>
 * </ul>
 *
 * @see com.example.ecommerce.service.impl.ShowcaseStrategyServiceImpl 本接口的默认实现
 * @see ProductMetricService 提供自动调优所需的指标数据
 * @see com.example.ecommerce.controller.ShowcaseStrategyController 调用本接口的控制器
 */
public interface ShowcaseStrategyService {

    /**
     * 获取当前商品展示策略配置。
     * <p>
     * 业务流程：
     * <ol>
     *   <li>从数据库加载策略配置（若不存在则创建默认配置）</li>
     *   <li>将配置同步到运行时的 Properties 对象（内存中的权重生效）</li>
     *   <li>构建响应 DTO，包含当前权重、时间窗口、指标汇总等信息</li>
     * </ol>
     * <p>
     * 设计思路：返回的 {@link ShowcaseStrategyResponse} 不仅包含当前权重配置，
     * 还包含短期/长期的指标汇总和最近 14 天的每日指标明细，
     * 这样前端可以在一个接口中获取"配置 + 数据"，减少请求次数。
     * </p>
     *
     * @return 策略响应 DTO（包含模式、权重、窗口配置、指标汇总等）
     */
    ShowcaseStrategyResponse getStrategy();

    /**
     * 保存商品展示策略配置。
     * <p>
     * 业务流程：
     * <ol>
     *   <li>校验请求参数（窗口天数范围、权重值合法性等）</li>
     *   <li>根据模式（MANUAL/AUTO）分别处理：
     *       <ul>
     *         <li>MANUAL：直接应用手动配置的权重，归一化后持久化</li>
     *         <li>AUTO：保存基础配置后立即执行自动调优</li>
     *       </ul>
     *   </li>
     *   <li>将配置同步到运行时 Properties 对象</li>
     *   <li>清除 Redis 中的商品展示缓存</li>
     *   <li>返回更新后的策略响应</li>
     * </ol>
     * <p>
     * 设计思路：MANUAL 和 AUTO 两种模式在同一方法中处理，对调用方透明。
     * AUTO 模式下保存配置后会自动触发一次调优，确保新配置立即生效。
     * </p>
     *
     * @param request 策略请求 DTO（包含模式、窗口天数、各维度权重等）
     * @return 更新后的策略响应 DTO
     * @throws com.example.ecommerce.common BusinessException 参数校验失败（400）时抛出
     */
    ShowcaseStrategyResponse saveStrategy(ShowcaseStrategyRequest request);

    /**
     * 立即执行自动调优（手动触发）。
     * <p>
     * 设计思路：管理员可以在后台看板点击"立即调优"按钮触发此方法。
     * 它会根据最新的商品指标数据重新计算所有维度的权重，并更新配置。
     * 仅在 AUTO 模式下可用，MANUAL 模式下调用会抛出异常。
     * </p>
     * <p>
     * 调优算法的核心思路是"动量分析" —— 比较短期窗口和长期窗口的指标趋势：
     * </p>
     * <ul>
     *   <li>若短期指标 > 长期指标 → 趋势向好 → 提高该维度权重</li>
     *   <li>若短期指标 < 长期指标 → 趋势向差 → 降低该维度权重</li>
     *   <li>使用指数移动平均（EMA）进行平滑，避免权重剧烈波动</li>
     * </ul>
     *
     * @return 调优后的策略响应 DTO
     * @throws com.example.ecommerce.common BusinessException 非 AUTO 模式时抛出（400）
     */
    ShowcaseStrategyResponse autoTuneNow();

    /**
     * 按需自动调优（定时任务触发）。
     * <p>
     * 设计思路：此方法由定时任务（ScheduledTaskService）定期调用，
     * 与 {@code autoTuneNow} 的区别在于：
     * </p>
     * <ul>
     *   <li>不返回响应 DTO（定时任务不需要返回值）</li>
     *   <li>非 AUTO 模式时静默返回（不抛异常，避免定时任务中断）</li>
     *   <li>异常被内部捕获并记录日志（不影响定时任务的后续执行）</li>
     * </ul>
     * <p>
     * 这种设计体现了"防御性编程"的思想 —— 定时任务是后台自动运行的，
     * 不应该因为一次调优失败就停止整个任务链。
     * </p>
     */
    void autoTuneIfEnabled();
}
