package com.example.ecommerce.controller;

import com.example.ecommerce.common.BusinessException;
import com.example.ecommerce.common.Result;
import com.example.ecommerce.config.AlipayProperties;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.security.CustomUserDetails;
import com.example.ecommerce.security.SecurityUtils;
import com.example.ecommerce.service.OrderService;
import com.example.ecommerce.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.tomcat.util.bcel.classfile.ConstantUtf8;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "支付接口", description = "支付创建、回调、查询")
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final AlipayProperties alipayProperties;

    public PaymentController(PaymentService paymentService, OrderService orderService, AlipayProperties alipayProperties) {
        this.paymentService = paymentService;
        this.orderService = orderService;
        this.alipayProperties = alipayProperties;
    }

    /**
     * 创建支付订单
     * 前端调用之后会拿到paymentUrl,可以通过window.location.href跳转到支付页面
     * 返回LinkedHashMap
     * @param orderNo 订单编号
     * @param amount 支付金额 可以为空
     * @param description 支付描述 可以为空
     * @param authentication 认证信息
     * @return 支付订单信息
     */
    @PostMapping("/create")
    public Result<Map<String,String>> createPayment(
            @RequestParam String orderNo,
            @RequestParam(required = false)BigDecimal amount,
            @RequestParam(required = false)String description,
            Authentication authentication){
        // 根据订单编号查询订单是否存在
        Order order = orderService.getByOrderNo(orderNo);
        if (order == null)
            throw new BusinessException(Result.NOT_FOUND_CODE, "订单信息不存在" + orderNo);
        // 防越权操作
        assertOrderAccess(authentication, order);
        // 使用返回的LinkedHashMap保证返回的JSON字段顺序是固定的，先是paymentUrl
        Map<String, String> response = new LinkedHashMap<>();
        response.put("paymentUrl", paymentService.createPayment(orderNo, amount, description));
        return Result.success(response);
    }

    @GetMapping(value = "/mock/success", produces = MediaType.TEXT_HTML_VALUE)
    public String mockSuccess(
            @RequestParam String orderNo,
            @RequestParam(required = false) String amount,
            @RequestParam(required = false) String subject){
        // Mock 支付开关守卫：未开启时拒绝访问（与测试期望的 403 对应）
        if (!alipayProperties.isMockEnabled()) {
            throw new BusinessException(Result.FORBIDDEN_CODE, "Mock 支付未开启");
        }
        Map<String,String> params = new LinkedHashMap<>();
        params.put("orderNo", orderNo);
        params.put("status", "SUCCESS");
        if (amount != null) params.put("amount", amount);
        // 模拟回调
        boolean success = paymentService.handleMockCallback(params);
        if (!success){
            // 跳转失败页
            return loadTemplate("templates/mock-payment-fail.html")
                    .replace("{{orderNo}}", escapeHtml(orderNo));
        }
        // 成功页：把模板中的 {{orderNo}} / {{amount}} / {{subject}} 占位符替换为真实值。
        // 所有替换值都先 escapeHtml，杜绝 XSS（模板是静态文件，不能信任外部参数）。
        return loadTemplate("templates/mock-payment-success.html")
                .replace("{{orderNo}}", escapeHtml(orderNo))
                .replace("{{amount}}", escapeHtml(amount == null ? "0.00" : amount))
                .replace("{{subject}}", escapeHtml(subject == null ? "-" : subject));
    }

    /**
     * 支付宝异步回调（服务端到服务端调用，无需用户登录，已在 SecurityConfig 放行）。
     * 透传原始回调参数给 PaymentService，并按支付宝规范原样返回纯文本 "success"/"failure"：
     *   - 返回 "success" 告知支付宝处理成功，停止重试；
     *   - 返回 "failure" 告知支付宝处理失败，支付宝会在 25h 内重试 8 次。
     */
    @PostMapping("/callback")
    public String handleCallback(@RequestParam Map<String, String> params) {
        boolean success = paymentService.handleCallback(params);
        return success ? "success" : "failure";
    }

    /**
     * HTML特殊字符的转义
     * @param value
     * @return
     */
    private CharSequence escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * 从classpath加载HTML模板
     * @param s 模板文件的相对路径
     * @return
     */
    private String loadTemplate(String s) {
        try {
            // 可以读取项目内的资源目录，例如 templates 目录
            ClassPathResource resource = new ClassPathResource(s);
            try (InputStream inputStream = resource.getInputStream()){
                // 以UTF-8编码读取字节流并转换为字符串
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new BusinessException(Result.ERROR_CODE, "模板读取失败" + s);
        }
    }


    /**
     * 越权检查，用户只能访问自己的订单，管理员可以访问所有订单
     * @param authentication
     * @param order
     */
    private void assertOrderAccess(Authentication authentication, Order order) {
        CustomUserDetails user = SecurityUtils.currentUser(authentication);
        if (!user.isAdmin() && !user.getId().equals(order.getUserId()))
            throw new BusinessException(Result.FORBIDDEN_CODE, "无权限访问");
    }
}
