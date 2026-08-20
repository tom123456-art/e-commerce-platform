package com.example.ecommerce.controller;

import com.example.ecommerce.common.Result;
import com.example.ecommerce.config.AlipayProperties;
import com.example.ecommerce.config.SecurityConfig;
import com.example.ecommerce.config.TestRedisConfig;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.security.CustomUserDetails;
import com.example.ecommerce.security.CustomUserDetailsService;
import com.example.ecommerce.security.TokenAuthenticationFilter;
import com.example.ecommerce.security.TokenService;
import com.example.ecommerce.service.OrderService;
import com.example.ecommerce.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PaymentController 切片测试（@WebMvcTest + MockMvc）。
 *
 * <p>测试目标：只验证【控制器层】的职责，不加载 PaymentService / OrderService / 数据库：</p>
 * <ul>
 *   <li>创建支付 POST /api/payment/create：订单归属校验（越权防护）、404、管理员可代操作、返回 paymentUrl</li>
 *   <li>异步回调 POST /api/payment/callback：透传参数给 Service，并原样返回 "success"/"failure" 纯文本</li>
 *   <li>Mock 支付 GET /api/payment/mock/success：mockEnabled=false 时拒绝（403），开启时渲染 HTML 并触发回调逻辑</li>
 * </ul>
 *
 * <p>与 OrderControllerTest 同一套鉴权写法：复用 TokenAuthenticationFilter + stub tokenService.parseToken，
 * 真实加载安全链路以验证越权防护；AlipayProperties 也 Mock 掉，方便精确控制 mockEnabled 开关。</p>
 */
@ActiveProfiles("test")
@WebMvcTest(PaymentController.class)
@org.springframework.context.annotation.Import({SecurityConfig.class, TokenAuthenticationFilter.class, TestRedisConfig.class})
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private OrderService orderService;

    /**
     * 支付宝配置属性：Mock 掉，以便精确控制 isMockEnabled() 开关
     */
    @MockitoBean
    private AlipayProperties alipayProperties;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private CustomUserDetails userDetails;
    private CustomUserDetails adminDetails;

    @BeforeEach
    void setUpTokens() {
        userDetails = new CustomUserDetails(
                2L, "user1", "password", 1,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        adminDetails = new CustomUserDetails(
                1L, "admin", "password", 1,
                Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        when(tokenService.parseToken("user-token")).thenReturn(userDetails);
        when(tokenService.parseToken("admin-token")).thenReturn(adminDetails);
    }

    // ==================== 创建支付：归属校验 ====================

    /**
     * 普通用户为自己的订单创建支付 → 200，并返回 paymentUrl。
     */
    @Test
    void createPayment_asUser_ownOrder_returnsPaymentUrl() throws Exception {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(2L);
        order.setOrderNo("ORDER-1");
        order.setTotalAmount(new BigDecimal("100.00"));
        when(orderService.getByOrderNo("ORDER-1")).thenReturn(order);
        when(paymentService.createPayment(eq("ORDER-1"), any(), any())).thenReturn("http://localhost:8080/api/payment/mock/success?orderNo=ORDER-1");

        mockMvc.perform(post("/api/payment/create")
                        .header("Authorization", "Bearer user-token")
                        .param("orderNo", "ORDER-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentUrl").value(containsString("mock/success")));

        verify(paymentService).createPayment(eq("ORDER-1"), any(), any());
    }

    /**
     * 订单不存在 → 404。
     */
    @Test
    void createPayment_orderNotFound_returns404() throws Exception {
        when(orderService.getByOrderNo("NOPE")).thenReturn(null);

        mockMvc.perform(post("/api/payment/create")
                        .header("Authorization", "Bearer user-token")
                        .param("orderNo", "NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(Result.NOT_FOUND_CODE));

        verify(paymentService, never()).createPayment(any(), any(), any());
    }

    /**
     * 普通用户为他人订单创建支付 → 403（水平越权防护）。
     */
    @Test
    void createPayment_otherUserOrder_returns403() throws Exception {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(99L);
        order.setOrderNo("ORDER-1");
        when(orderService.getByOrderNo("ORDER-1")).thenReturn(order);

        mockMvc.perform(post("/api/payment/create")
                        .header("Authorization", "Bearer user-token")
                        .param("orderNo", "ORDER-1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(Result.FORBIDDEN_CODE));

        verify(paymentService, never()).createPayment(any(), any(), any());
    }

    /**
     * 管理员为他人订单创建支付 → 200（管理员不受归属限制）。
     */
    @Test
    void createPayment_asAdmin_otherOrder_returnsOk() throws Exception {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(99L);
        order.setOrderNo("ORDER-1");
        order.setTotalAmount(new BigDecimal("100.00"));
        when(orderService.getByOrderNo("ORDER-1")).thenReturn(order);
        when(paymentService.createPayment(eq("ORDER-1"), any(), any())).thenReturn("http://localhost:8080/api/payment/mock/success?orderNo=ORDER-1");

        mockMvc.perform(post("/api/payment/create")
                        .header("Authorization", "Bearer admin-token")
                        .param("orderNo", "ORDER-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentUrl").value(containsString("mock/success")));
    }

    // ==================== 异步回调：纯文本响应 ====================

    /**
     * 回调处理成功 → 控制器原样返回纯文本 "success"（注意不是 JSON）。
     */
    @Test
    void callback_returnsSuccessString() throws Exception {
        when(paymentService.handleCallback(anyMap())).thenReturn(true);

        mockMvc.perform(post("/api/payment/callback")
                        .param("out_trade_no", "ORDER-1")
                        .param("trade_status", "TRADE_SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));

        // 验证 Service 收到的参数里带了商户订单号
        verify(paymentService).handleCallback(argThat(m -> "ORDER-1".equals(m.get("out_trade_no"))));
    }

    /**
     * 回调处理失败 → 返回纯文本 "failure"。
     */
    @Test
    void callback_returnsFailureString() throws Exception {
        when(paymentService.handleCallback(anyMap())).thenReturn(false);

        mockMvc.perform(post("/api/payment/callback")
                        .param("out_trade_no", "ORDER-1"))
                .andExpect(status().isOk())
                .andExpect(content().string("failure"));
    }

    // ==================== Mock 支付：开关控制 + HTML 渲染 ====================

    /**
     * mockEnabled=true 时访问 Mock 支付成功页 → 200，返回 HTML 且含订单号，并触发回调逻辑。
     */
    @Test
    void mockSuccess_whenEnabled_returnsHtmlAndTriggersCallback() throws Exception {
        when(alipayProperties.isMockEnabled()).thenReturn(true);
        when(paymentService.handleMockCallback(anyMap())).thenReturn(true);

        mockMvc.perform(get("/api/payment/mock/success")
                        .header("Authorization", "Bearer user-token")
                        .param("orderNo", "ORDER-1")
                        .param("amount", "100.00")
                        .param("subject", "手机"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("ORDER-1")));

        // 控制器构造的模拟回调参数：含 orderNo 与固定 status=SUCCESS
        verify(paymentService).handleMockCallback(argThat(m ->
                "ORDER-1".equals(m.get("orderNo")) && "SUCCESS".equals(m.get("status"))));
    }

    /**
     * mockEnabled=false 时访问 Mock 支付 → 403 拒绝，且不触发回调逻辑。
     * （你的项目 enabled=false、mockEnabled=true，因此这条走的是"开启"分支；此用例验证关闭开关的防护。）
     */
    @Test
    void mockSuccess_whenDisabled_returns403() throws Exception {
        when(alipayProperties.isMockEnabled()).thenReturn(false);

        mockMvc.perform(get("/api/payment/mock/success")
                        .header("Authorization", "Bearer user-token")
                        .param("orderNo", "ORDER-1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(Result.FORBIDDEN_CODE));

        verify(paymentService, never()).handleMockCallback(any());
    }
}
