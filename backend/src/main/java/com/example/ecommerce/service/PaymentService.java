package com.example.ecommerce.service;

import com.example.ecommerce.entity.PaymentCallbackLog;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentService {
    /**
     * 创建支付链接（真实支付宝 URL 或 Mock URL）
     *
     * @param orderNo     本系统订单号。
     *                    为什么要传：PaymentService 据此查出订单（校验订单存在、取订单金额、校验支付归属），
     *                    是创建支付的入口标识，没有它就无法定位要支付哪笔订单。
     * @param amount      前端传入的支付金额（可空）。
     *                    为什么要传：用于与订单金额做一致性校验，防止前端篡改金额发起"少付"；
     *                    为空时以订单实际金额为准，给前端不传金额留余地。
     * @param description 支付标题/商品描述（可空）。
     *                    为什么要传：展示在支付宝收银台的订单标题；为空时自动用
     *                    "Order Payment-{orderNo}" 兜底，保证收银台始终有可读标题。
     */
    String createPayment(String orderNo, BigDecimal amount, String description);

    /**
     * 处理支付宝异步回调（验签 + 金额校验 + 状态更新 + 审计日志）
     *
     * @param callbackParams 支付宝异步回调的原始参数 Map（全部字段，含 out_trade_no / trade_no /
     *                       trade_status / 签名 sign / total_amount 等）。
     *                       为什么要传：回调处理每一步——验签、金额校验、订单状态更新——都依赖这些
     *                       原始字段，必须整包传入；且审计日志要把它原样留存作取证依据。
     */
    boolean handleCallback(Map<String, String> callbackParams);

    /**
     * Mock 支付回调（开发环境，跳过验签）
     *
     * @param callbackParams Mock 回调参数 Map（含 orderNo/out_trade_no、status/trade_status、amount 等）。
     *                       为什么要传：与真实回调同理，Mock 模式下也用同一套字段做金额校验与状态更新，
     *                       保持两条路径业务逻辑一致，只是验签环节被 IP+Header 白名单替代。
     */
    boolean handleMockCallback(Map<String, String> callbackParams);

    // ┌────────────────────────────────────────────────────────── ── ── ──
    // │ 🟢 修改点1 [新增]：createCallbackLog 接口方法
    // │ 模板写法：无此接口方法（createCallbackLog 为 PaymentServiceImpl 内的 private 方法）
    // │ 文档写法：public 接口方法，便于通过 self 代理调用激活 REQUIRES_NEW 独立事务
    // │ 修改原因：将审计日志方法提升为接口契约，支持 self 代理调用
    // └────────────────────────────────────────────────────────── ── ── ──

    /**
     * 创建支付回调审计日志（独立事务，保证即使主事务回滚日志依然落库）
     *
     * @param orderNo     本系统商户订单号（由支付宝回调的 out_trade_no 提取）。
     *                    为什么要传：审计日志必须能关联到具体订单，否则日志只是一串无意义的记录；
     *                    后续排查"某笔订单的支付回调到底发生了什么"就靠它定位。
     * @param tradeNo     支付宝交易号（回调的 trade_no）。
     *                    为什么要传：它是支付宝侧这笔交易的唯一凭证，与 orderNo 形成
     *                    "本系统订单 ↔ 支付宝交易"的对照关系，对账、退款、争议处理时都要用。
     * @param tradeStatus 支付宝告知的交易状态（TRADE_SUCCESS / TRADE_FINISHED / SUCCESS 等）。
     *                    为什么要传：把"支付宝认为这笔交易是什么结果"原样落库，
     *                    便于事后比对"支付宝的结果"与"我们最终的处理结果"是否一致。
     * @param params      支付宝回调的原始参数 Map（POST 过来的全部字段，含签名、金额等）。
     *                    为什么要传：完整保留"当时支付宝到底发了什么"，序列化为 rawPayload 存库，
     *                    不丢任何字段——出现验签失败 / 金额不一致等问题时，这是唯一的取证依据。
     * @return 创建后的审计日志对象（含自增 ID），供后续 updateCallbackLog 定位同一条记录
     */
    PaymentCallbackLog createCallbackLog(String orderNo, String tradeNo, String tradeStatus,
                                         Map<String, String> params);

    // ┌────────────────────────────────────────────────────────── ── ── ──
    // │ 🟢 修改点2 [新增]：updateCallbackLog 接口方法
    // │ 模板写法：无此接口方法（updateCallbackLog 为 PaymentServiceImpl 内的 private 方法）
    // │ 文档写法：public 接口方法，便于通过 self 代理调用激活 REQUIRES_NEW 独立事务
    // │ 修改原因：将审计日志方法提升为接口契约，支持 self 代理调用
    // └────────────────────────────────────────────────────────── ── ── ──

    /**
     * 更新支付回调审计日志的处理结果（独立事务）
     *
     * @param cbLog        上一步 createCallbackLog 返回的审计日志对象（含自增 id）。
     *                     为什么要传：明确"更新哪一条"记录；在 REQUIRES_NEW 独立事务里它已是持久化对象，靠 id 定位。
     * @param verified     验签是否通过（true=支付宝签名验证通过，来源可信）。
     *                     为什么要传：这是安全审计的关键字段，记录这笔回调来源是否合法。
     * @param processed    业务是否已处理（true=已更新订单状态/记指标等）。
     *                     为什么要传：区分"收到并验签了"和"真正处理完了"两个阶段，避免重复统计或误判。
     * @param success      本次处理是否最终成功。
     *                     为什么要传：最外层的结果标记，运维/运营一眼就能看出这笔回调最终是成功还是失败。
     * @param errorMessage 处理失败时的错误原因（成功时为 null）。
     *                     为什么要传：失败时把异常信息记下来，便于定位问题，否则日志只标"失败"却不知为何失败。
     */
    void updateCallbackLog(PaymentCallbackLog cbLog, boolean verified, boolean processed,
                           boolean success, String errorMessage);
}