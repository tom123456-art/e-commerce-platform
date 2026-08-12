package com.example.ecommerce.service.impl;

import com.example.ecommerce.common.BusinessException;
import com.example.ecommerce.common.Result;
import com.example.ecommerce.config.AlipayProperties;
import com.example.ecommerce.dto.OrderDetailResponse;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.PaymentCallbackLog;
import com.example.ecommerce.mapper.PaymentCallbackLogMapper;
import com.example.ecommerce.messaging.OrderMessagePublisher;
import com.example.ecommerce.service.OrderService;
import com.example.ecommerce.service.PaymentService;
import com.example.ecommerce.service.ProductMetricService;
import com.example.ecommerce.utils.AlipaySignatureUtils;
import com.example.ecommerce.utils.SensitiveDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// ┌────────────────────────────────────────────────────────── ── ── ──
// │ ⚙️ 修改点4 [配套]：新增 3 个 import（Autowired、Lazy、Propagation）
// │ 模板写法：无这 3 个 import
// │ 文档写法：import Autowired / Lazy / Propagation
// │ 修改原因：配套 self 自注入字段（需 @Autowired + @Lazy）与 REQUIRES_NEW 事务（需 Propagation）
// └────────────────────────────────────────────────────────── ── ── ──
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final DateTimeFormatter ALIPAY_TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    /** Mock 回调允许的 IP 白名单（仅本机回环） */
    private static final String[] MOCK_ALLOWED_IPS = {"127.0.0.1", "0:0:0:0:0:0:0:1", "::1"};
    /** Mock 回调内部调用标识 Header */
    private static final String MOCK_INTERNAL_HEADER = "X-Mock-Internal-Call";

    private final AlipayProperties alipayProperties;
    private final OrderService orderService;
    private final OrderMessagePublisher orderMessagePublisher;
    private final PaymentCallbackLogMapper paymentCallbackLogMapper;
    private final ProductMetricService productMetricService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 自注入代理对象（Self-Injection），用于解决 Spring AOP 自调用失效问题。
     *
     * 【为什么需要 self？】
     *   handleCallback() 中的 try-finally 调用 createCallbackLog() / updateCallbackLog()，
     *   如果直接用 this.createCallbackLog()（自调用），Spring AOP 代理不生效，
     *   @Transactional(propagation = REQUIRES_NEW) 注解会被忽略，审计日志仍与主事务绑定。
     *   通过注入自身代理对象 self，调用 self.createCallbackLog() 走代理，REQUIRES_NEW 才能生效。
     *
     * 【为什么用 @Lazy？】
     *   PaymentServiceImpl 在构造时注入 PaymentService（即自身），会形成循环依赖：
     *   "我创建时需要我自己"。@Lazy 延迟注入：构造时注入一个代理占位符，
     *   首次实际调用时才从容器中解析真正的 Bean，打破启动期的循环依赖。
     */
    // ┌────────────────────────────────────────────────────────── ── ── ──
    // │ 🔵 修改点1 [重构]：新增 self 自注入字段
    // │ 模板写法：无 self 字段（直接 this 调用 createCallbackLog/updateCallbackLog）
    // │ 文档写法：@Lazy @Autowired private PaymentService self;
    // │ 修改原因：通过注入自身代理对象，调用 self.xxx() 走 Spring AOP 代理，激活 @Transactional(REQUIRES_NEW)
    // └────────────────────────────────────────────────────────── ── ── ──
    @Lazy
    @Autowired
    private PaymentService self;

    public PaymentServiceImpl(AlipayProperties alipayProperties,
                              OrderService orderService,
                              OrderMessagePublisher orderMessagePublisher,
                              PaymentCallbackLogMapper paymentCallbackLogMapper,
                              ProductMetricService productMetricService) {
        this.alipayProperties = alipayProperties;
        this.orderService = orderService;
        this.orderMessagePublisher = orderMessagePublisher;
        this.paymentCallbackLogMapper = paymentCallbackLogMapper;
        this.productMetricService = productMetricService;
    }

    /**
     * 【创建支付】根据配置选择真实支付宝或 Mock 模式。
     *
     * 决策优先级：
     *   1. 真实支付宝参数齐全 → 生成签名 URL（用户跳转到支付宝收银台）
     *   2. mockEnabled=true → 生成 Mock 支付 URL（跳转到本地 /api/payment/mock/success）
     *   3. 两者都不满足 → 抛异常提示配置
     *
     * @param orderNo    本系统订单号（同接口定义）。实现里先用它查出 Order，校验订单存在并取订单金额。
     * @param amount     前端传入的支付金额（可空）。实现里交给 resolvePaymentAmount 与订单金额比对，防篡改。
     * @param description 收银台展示的订单标题（可空）。实现里为空时用 "Order Payment-{orderNo}" 兜底。
     */
    @Override
    public String createPayment(String orderNo, BigDecimal amount, String description) {
        Order order = orderService.getByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(Result.NOT_FOUND_CODE, "Order does not exist: " + orderNo);
        }
        // 金额校验：前端传了金额必须与订单金额一致（防篡改）
        BigDecimal totalAmount = resolvePaymentAmount(order, amount);
        String subject = (description == null || description.trim().isEmpty())
                ? "Order Payment-" + orderNo : description;

        if (isRealAlipayReady()) {
            return buildSignedAlipayUrl(orderNo, totalAmount, subject);
        }
        if (alipayProperties.isMockEnabled()) {
            return buildMockPaymentUrl(orderNo, totalAmount, subject);
        }
        throw new BusinessException(Result.BAD_REQUEST_CODE, "请先配置支付宝沙箱参数");
    }

    /**
     * 【支付宝异步回调】支付流程的终点。
     *
     * 处理流程：
     *   1. 提取关键参数（订单号、交易号、状态）
     *   2. 创建审计日志（初始状态：未验证、未处理、未成功）
     *   3. 验签：真实模式用 AlipaySignatureUtils.verify，Mock 模式用 IP+Header 校验
     *   4. 金额校验：回调金额必须与订单金额一致
     *   5. 幂等性：已支付的订单不重复更新
     *   6. 更新订单状态为已支付
     *   7. 记录商品销售指标（首次支付成功）
     *   8. 发送支付状态 MQ 事件
     *   9. 更新审计日志（无论成功失败都记录）
     *
     * 【返回值】true 返回 "success" 给支付宝（停止重试），false 返回 "failure"（25h 内重试 8 次）
     *
     * @param callbackParams 支付宝异步回调的原始参数 Map（同接口定义）。
     *                       实现里先从中提取 orderNo/tradeNo/tradeStatus，再依次做
     *                       建审计日志 → 验签 → 金额校验 → 幂等更新 → 记指标 → 发 MQ → 更新审计日志。
     */
    @Override
    @Transactional
    public boolean handleCallback(Map<String, String> callbackParams) {
        String orderNo = firstNonEmpty(callbackParams.get("out_trade_no"), callbackParams.get("orderNo"));
        String tradeNo = callbackParams.get("trade_no");
        String tradeStatus = firstNonEmpty(callbackParams.get("trade_status"), callbackParams.get("status"));

        log.info("收到支付回调 - 订单号: {}, 交易号: {}, 状态: {}",
                SensitiveDataUtil.mask(orderNo, 4, 4, '*'),
                SensitiveDataUtil.mask(tradeNo, 4, 4, '*'),
                tradeStatus);

        // ┌────────────────────────────────────────────────────────── ── ── ──
        // │ 🔵 修改点2 [重构]：通过 self 代理调用 createCallbackLog（handleCallback 中）
        // │ 模板写法：createCallbackLog(...) 直接调用（this 自调用，AOP 代理不生效，REQUIRES_NEW 失效）
        // │ 文档写法：self.createCallbackLog(...) 走代理调用，REQUIRES_NEW 独立事务生效
        // │ 修改原因：审计日志需独立于主事务，主事务回滚时日志仍能落库
        // └────────────────────────────────────────────────────────── ── ── ──
        // 创建审计日志（初始状态）—— 通过 self 代理调用，确保 REQUIRES_NEW 独立事务生效
        PaymentCallbackLog cbLog = self.createCallbackLog(orderNo, tradeNo, tradeStatus, callbackParams);

        boolean success = false;
        boolean newlyPaid = false;
        Order order = null;
        String errorMessage = null;

        try {
            // 验签：真实模式用 RSA2，Mock 模式用 IP+Header
            boolean verified = isRealAlipayReady()
                    ? AlipaySignatureUtils.verify(callbackParams, alipayProperties.getPublicKey())
                    : alipayProperties.isMockEnabled() && isMockCallbackAllowed();

            boolean tradeSuccessful = verified && (
                    "TRADE_SUCCESS".equals(tradeStatus)
                            || "TRADE_FINISHED".equals(tradeStatus)
                            || "SUCCESS".equals(tradeStatus));

            if (tradeSuccessful && orderNo != null) {
                // 正常处理订单：验签通过 + 交易成功 + 订单号存在
                order = validateCallbackAmount(orderNo, callbackParams);
                // 【幂等保护】只有当前状态不是"已支付"(>=1) 才更新，避免支付宝重复回调导致重复改状态/重复记指标
                if (!isPaidStatus(order.getStatus())) {
                    orderService.updateStatusByOrderNo(orderNo, 1);    // 把状态 0(待支付) → 1(已支付)
                    newlyPaid = true;
                }
                // 仅在"本次首次支付成功"时记录商品销售指标（recordPaymentSuccess 内部通常也做幂等）
                if (newlyPaid && order.getId() != null) {
                    OrderDetailResponse detail = orderService.getDetailById(order.getId());
                    productMetricService.recordPaymentSuccess(order, detail == null ? null : detail.getOrderItemList());
                }
                success = true;    // 正常处理完成才标记成功
            } else if (tradeSuccessful && orderNo == null) {
                // 【资金安全】支付成功但缺少订单号，无法关联订单。
                // 必须返回 failure 让支付宝重试回调，否则这笔钱将"无主"。
                // 旧代码此处误把 success 设为 true（tradeSuccessful），导致支付宝停止重试，资金风险。
                log.warn("支付回调成功但缺少订单号，无法处理");
                success = false;
                errorMessage = "Missing order number";
            }
            // 其他情况（tradeSuccessful=false）：success 保持初始值 false

            // 发送支付状态 MQ 事件
            if (orderNo != null) {
                orderMessagePublisher.publishPaymentStatus(orderNo, tradeStatus, success);
            }
            return success;
        } catch (Exception ex) {
            success = false;
            errorMessage = ex.getMessage();
            throw ex;
        } finally {
            // ┌────────────────────────────────────────────────────────── ── ── ──
            // │ 🔵 修改点2 [重构]：通过 self 代理调用 updateCallbackLog（handleCallback finally）
            // │ 模板写法：updateCallbackLog(...) 直接调用（this 自调用，AOP 代理不生效）
            // │ 文档写法：self.updateCallbackLog(...) 走代理调用，REQUIRES_NEW 独立事务生效
            // │ 修改原因：finally 块中无论主事务成功失败都需独立落库审计日志
            // └────────────────────────────────────────────────────────── ── ── ──
            // 无论成功失败都更新审计日志 —— 通过 self 代理调用，REQUIRES_NEW 确保日志独立落库
            self.updateCallbackLog(cbLog, true, true, success, errorMessage);
        }
    }

    /**
     * Mock 支付回调：开发环境用，跳过验签（用 IP+Header 白名单替代 RSA2 验签）。
     *
     * @param callbackParams Mock 回调参数 Map（同接口定义），含 orderNo/out_trade_no、
     *                       status/trade_status、amount 等。实现里按真实回调同样的流程
     *                       （金额校验→幂等更新→记指标）处理，只是验签环节被省略。
     */
    @Override
    @Transactional
    public boolean handleMockCallback(Map<String, String> callbackParams) {
        if (!alipayProperties.isMockEnabled()) return false;

        String orderNo = firstNonEmpty(callbackParams.get("out_trade_no"), callbackParams.get("orderNo"));
        String tradeStatus = firstNonEmpty(callbackParams.get("trade_status"), callbackParams.get("status"));
        log.info("Mock 支付回调 - 订单号: {}", SensitiveDataUtil.mask(orderNo, 4, 4, '*'));

        // ┌────────────────────────────────────────────────────────── ── ── ──
        // │ 🔵 修改点2 [重构]：通过 self 代理调用 createCallbackLog（handleMockCallback 中）
        // │ 模板写法：createCallbackLog(...) 直接调用（this 自调用，AOP 代理不生效）
        // │ 文档写法：self.createCallbackLog(...) 走代理调用，REQUIRES_NEW 独立事务生效
        // │ 修改原因：Mock 回调路径同样需独立事务保证审计日志落库
        // └────────────────────────────────────────────────────────── ── ── ──
        PaymentCallbackLog cbLog = self.createCallbackLog(orderNo, null, tradeStatus, callbackParams);
        boolean success = false;
        try {
            boolean tradeSuccessful = "TRADE_SUCCESS".equals(tradeStatus)
                    || "TRADE_FINISHED".equals(tradeStatus)
                    || "SUCCESS".equals(tradeStatus);
            if (tradeSuccessful && orderNo != null) {
                Order order = validateCallbackAmount(orderNo, callbackParams);
                // 同样做幂等保护：已支付则跳过改状态与记指标
                if (!isPaidStatus(order.getStatus())) {
                    orderService.updateStatusByOrderNo(orderNo, 1);
                    OrderDetailResponse detail = orderService.getDetailById(order.getId());
                    productMetricService.recordPaymentSuccess(order, detail == null ? null : detail.getOrderItemList());
                }
            }
            success = tradeSuccessful;
            // Mock 模式下也用 self 代理独立落库审计日志（REQUIRES_NEW），保证日志与主事务解耦
            self.updateCallbackLog(cbLog, true, true, success, null);
            return success;
        } catch (Exception ex) {
            self.updateCallbackLog(cbLog, true, true, false, ex.getMessage());
            throw ex;
        }
    }

    // ==================== 私有辅助方法 ====================

    // ┌────────────────────────────────────────────────────────── ── ── ──
    // │ 🔵 修改点3 [重构]：createCallbackLog 方法签名变更
    // │ 模板写法：private 无 @Override 无 @Transactional（自调用，事务注解不生效）
    // │ 文档写法：public + @Override + @Transactional(propagation = REQUIRES_NEW)
    // │ 修改原因：提升为接口契约的 public 方法，配合 self 代理调用激活 REQUIRES_NEW 独立事务
    // └────────────────────────────────────────────────────────── ── ── ──
    /**
     * 创建审计日志（初始状态：未验证、未处理、未成功）。
     * 参数含义与"为什么传"与接口 {@link PaymentService#createCallbackLog} 一致，此处实现负责真正落库：
     *   - orderNo/tradeNo/tradeStatus 写入对应列，建立订单与支付宝交易的对照
     *   - params 整体序列化为 rawPayload 存库，保留回调原始全量字段用于取证
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentCallbackLog createCallbackLog(String orderNo, String tradeNo, String tradeStatus,
                                                Map<String, String> params) {
        PaymentCallbackLog cbLog = new PaymentCallbackLog();
        cbLog.setOrderNo(orderNo);
        cbLog.setTradeNo(tradeNo);
        cbLog.setTradeStatus(tradeStatus);
        cbLog.setVerified(false);
        cbLog.setProcessed(false);
        cbLog.setSuccess(false);
        try {
            cbLog.setRawPayload(objectMapper.writeValueAsString(params));
        } catch (Exception ex) {
            cbLog.setRawPayload(String.valueOf(params));
        }
        paymentCallbackLogMapper.insert(cbLog);
        return cbLog;
    }

    // ┌────────────────────────────────────────────────────────── ── ── ──
    // │ 🔵 修改点3 [重构]：updateCallbackLog 方法签名变更
    // │ 模板写法：private 无 @Override 无 @Transactional（自调用，事务注解不生效）
    // │ 文档写法：public + @Override + @Transactional(propagation = REQUIRES_NEW)
    // │ 修改原因：提升为接口契约的 public 方法，配合 self 代理调用激活 REQUIRES_NEW 独立事务
    // └────────────────────────────────────────────────────────── ── ── ──
    /**
     * 更新审计日志（处理结果）。
     * 参数含义与"为什么传"与接口 {@link PaymentService#updateCallbackLog} 一致，此处实现负责把
     * verified/processed/success/errorMessage 写回同一条记录（按 cbLog.id 定位）。
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateCallbackLog(PaymentCallbackLog cbLog, boolean verified, boolean processed,
                                  boolean success, String errorMessage) {
        if (cbLog == null || cbLog.getId() == null) return;
        cbLog.setVerified(verified);
        cbLog.setProcessed(processed);
        cbLog.setSuccess(success);
        cbLog.setErrorMessage(errorMessage);
        paymentCallbackLogMapper.updateResult(cbLog);
    }

    /**
     * 构建签名的支付宝网关 URL（用户跳转到的收银台地址）。
     *
     * @param orderNo     商户订单号，作为 biz_content 里的 out_trade_no，支付宝回调用它关联订单。
     * @param totalAmount 实际支付金额（已通过 resolvePaymentAmount 校验，与订单金额一致）。
     * @param subject     收银台展示的订单标题。
     */
    private String buildSignedAlipayUrl(String orderNo, BigDecimal totalAmount, String subject) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("app_id", alipayProperties.getAppId());
        params.put("method", "alipay.trade.page.pay");
        params.put("format", "JSON");
        params.put("charset", "utf-8");
        params.put("sign_type", "RSA2");
        params.put("timestamp", LocalDateTime.now().format(ALIPAY_TS_FMT));
        params.put("version", "1.0");
        params.put("notify_url", alipayProperties.getNotifyUrl());
        params.put("return_url", alipayProperties.getReturnUrl());
        params.put("biz_content", buildBizContent(orderNo, totalAmount, subject));
        params.put("sign", AlipaySignatureUtils.sign(params, alipayProperties.getPrivateKey()));
        return alipayProperties.getGatewayUrl() + "?" + AlipaySignatureUtils.buildQuery(params);
    }

    /**
     * 构建支付宝业务参数 JSON（即开放接口要求的 biz_content 字段内容）。
     *
     * @param orderNo     商户订单号 → 映射为 out_trade_no（支付宝侧唯一业务号）。
     * @param totalAmount 支付金额 → 映射为 total_amount（字符串，避免浮点精度问题）。
     * @param subject     订单标题 → 映射为 subject（收银台展示给用户看）。
     */
    private String buildBizContent(String orderNo, BigDecimal totalAmount, String subject) {
        try {
            Map<String, Object> biz = new LinkedHashMap<>();
            biz.put("out_trade_no", orderNo);
            biz.put("product_code", "FAST_INSTANT_TRADE_PAY");
            biz.put("total_amount", totalAmount.toPlainString());
            biz.put("subject", subject);
            return objectMapper.writeValueAsString(biz);
        } catch (Exception ex) {
            throw new BusinessException(Result.ERROR_CODE, "Failed to build Alipay request payload");
        }
    }

    /**
     * 构建 Mock 支付页面 URL（开发环境跳转到本地 /api/payment/mock/success）。
     *
     * @param orderNo 商户订单号 → 作为 URL 的 orderNo 查询参数，Mock 端点据此查订单。
     * @param amount  支付金额 → 作为 amount 查询参数回传，便于 Mock 端点做金额校验演示。
     * @param subject 订单标题 → 作为 subject 查询参数，用于成功页展示。
     */
    private String buildMockPaymentUrl(String orderNo, BigDecimal amount, String subject) {
        String callbackUrl = alipayProperties.getNotifyUrl() != null && alipayProperties.getNotifyUrl().contains("/callback")
                ? alipayProperties.getNotifyUrl().replace("/callback", "/mock/success")
                : "http://localhost:8080/api/payment/mock/success";
        return callbackUrl
                + "?orderNo=" + urlEncode(orderNo)
                + "&amount=" + urlEncode(amount.toPlainString())
                + "&subject=" + urlEncode(subject);
    }

    /**
     * 校验"前端传入的支付金额"与"订单实际金额"一致（创建支付环节，防前端篡改少付）。
     *
     * @param order            已查出的订单对象，用于取订单实际金额 order.getTotalAmount()。
     * @param requestedAmount  前端传入的支付金额（可空）。为空说明前端信任订单金额，直接放行。
     * @return 订单实际金额（始终以订单金额为准，忽略前端的金额参与后续签名/跳转）。
     */
    private BigDecimal resolvePaymentAmount(Order order, BigDecimal requestedAmount) {
        BigDecimal orderAmount = order.getTotalAmount();
        if (requestedAmount != null && orderAmount != null && requestedAmount.compareTo(orderAmount) != 0) {
            throw new BusinessException(Result.BAD_REQUEST_CODE, "支付金额与订单金额不一致");
        }
        return orderAmount;
    }

    /**
     * 校验"支付宝回调里的金额"与"订单实际金额"一致（回调环节，防金额篡改攻击），并返回订单。
     *
     * @param orderNo 商户订单号，用于查出订单对象（同时校验订单存在）。
     * @param params  回调原始参数 Map，从中取 total_amount/amount 作为支付宝告知的金额。
     * @return 校验通过后的 Order 对象，供上层更新状态、记销售指标使用。
     */
    private Order validateCallbackAmount(String orderNo, Map<String, String> params) {
        String amountText = firstNonEmpty(params.get("total_amount"), params.get("amount"));
        Order order = orderService.getByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(Result.NOT_FOUND_CODE, "Order does not exist: " + orderNo);
        }
        if (!hasText(amountText)) return order;
        BigDecimal callbackAmount = new BigDecimal(amountText);
        if (order.getTotalAmount() != null && callbackAmount.compareTo(order.getTotalAmount()) != 0) {
            throw new BusinessException(Result.CONFLICT_CODE, "回调金额与订单金额不一致");
        }
        return order;
    }

    /** Mock 回调安全校验：IP 白名单 + 内部调用标识 */
    private boolean isMockCallbackAllowed() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attrs == null ? null : attrs.getRequest();
        String clientIp = request == null ? null : request.getRemoteAddr();
        String internalHeader = request == null ? null : request.getHeader(MOCK_INTERNAL_HEADER);

        boolean ipAllowed = false;
        if (clientIp != null) {
            for (String allowed : MOCK_ALLOWED_IPS) {
                if (allowed.equals(clientIp)) { ipAllowed = true; break; }
            }
        }
        boolean headerValid = "true".equalsIgnoreCase(internalHeader);
        if (!ipAllowed || !headerValid) {
            log.warn("[安全告警] Mock 支付回调被拒绝 - IP: {}, Header: {}", clientIp, internalHeader);
            return false;
        }
        return true;
    }

    /**
     * 判断"真实支付宝模式"是否可用：enabled 开关打开且 appId/私钥/公钥/网关地址都已配置。
     * 任一缺失都视为未就绪，此时若 mockEnabled=true 会降级到 Mock 模式。
     */
    private boolean isRealAlipayReady() {
        return alipayProperties.isEnabled()
                && hasText(alipayProperties.getAppId())
                && hasText(alipayProperties.getPrivateKey())
                && hasText(alipayProperties.getPublicKey())
                && hasText(alipayProperties.getGatewayUrl());
    }

    /**
     * 对 URL 参数值做 UTF-8 百分号编码。
     *
     * @param value 待编码的原始字符串（如 orderNo、金额、标题）。
     *              为什么要传：拼接到查询字符串前必须编码，否则中文/特殊字符会破坏 URL；
     *              且把 + 替换回 %20，与支付宝/浏览器对空格的约定保持一致。
     */
    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (Exception ex) {
            throw new IllegalStateException("UTF-8 is not supported", ex);
        }
    }

    private boolean hasText(String v) { return v != null && !v.trim().isEmpty(); }
    private boolean isPaidStatus(Integer s) { return s != null && s >= 1; }
    private String firstNonEmpty(String a, String b) { return hasText(a) ? a : b; }
}