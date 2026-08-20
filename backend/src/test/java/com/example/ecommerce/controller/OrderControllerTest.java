package com.example.ecommerce.controller;

import com.example.ecommerce.common.Result;
import com.example.ecommerce.config.SecurityConfig;
import com.example.ecommerce.config.TestRedisConfig;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.security.CustomUserDetails;
import com.example.ecommerce.security.CustomUserDetailsService;
import com.example.ecommerce.security.TokenAuthenticationFilter;
import com.example.ecommerce.security.TokenService;
import com.example.ecommerce.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OrderController 切片测试（@WebMvcTest + MockMvc）。
 *
 * <p>测试目标：只验证【控制器层】的职责，不加载业务 Service / DB：</p>
 * <ul>
 *   <li>统一返回结构：所有接口都包成 {@code Result<T>}，异常由全局处理器映射成 code + 404/403/409</li>
 *   <li><b>越权防护（水平越权）</b>：普通用户只能访问自己的订单，访问他人订单返回 403</li>
 *   <li><b>角色分流</b>：管理员看全部订单，普通用户只看自己的；管理员可改任意字段，普通用户只能确认收货</li>
 *   <li><b>状态机 + 幂等</b>：普通用户确认收货必须是 1→2，重复确认不报错，非法状态转换被拒</li>
 *   <li><b>userId 强制绑定</b>：普通用户下单时无论前端传什么 userId，都被覆盖为当前登录用户</li>
 * </ul>
 *
 * <p>认证机制：复用项目的 TokenAuthenticationFilter —— 它从 {@code Authorization: Bearer <token>}
 * 取出 token，交给 {@code TokenService.parseToken} 还原出 {@code CustomUserDetails} 作为认证主体。
 * 因此测试里只需 stub {@code tokenService.parseToken("user-token"/"admin-token")} 即可模拟登录态，
 * 与真实运行时的鉴权路径一致（这也是 {@code ApiControllerInterfaceTest} 使用的同一套写法）。</p>
 */
@ActiveProfiles("test")
@WebMvcTest(OrderController.class)
@org.springframework.context.annotation.Import({SecurityConfig.class, TokenAuthenticationFilter.class, TestRedisConfig.class})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 被 Mock 的订单服务，所有业务逻辑均由 stub 驱动，不触达数据库
     */
    @MockitoBean
    private OrderService orderService;

    /**
     * 被 Mock 的 Token 解析服务，用于注入"已登录用户"身份
     */
    @MockitoBean
    private TokenService tokenService;

    /**
     * 被 Mock 的用户详情服务：SecurityConfig 的 authenticationProvider 依赖它，测试中不触达真实实现
     */
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    /**
     * 普通用户：id=2，只有 ROLE_USER
     */
    private CustomUserDetails userDetails;
    /**
     * 管理员：id=1，拥有 ROLE_USER + ROLE_ADMIN
     */
    private CustomUserDetails adminDetails;

    /**
     * 预置两类身份：用同样的 stub 把 "user-token" / "admin-token" 解析成对应 CustomUserDetails。
     * 之后请求只要带上 {@code Authorization: Bearer user-token} 即视为普通用户已登录。
     */
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

    // ==================== 列表查询：角色分流 ====================

    /**
     * 普通用户 GET /api/orders → 只查自己的订单（getByUserId）。
     * 断言：调用的是 orderService.getByUserId(2)，而非 getAll。
     */
    @Test
    void getAll_asUser_returnsOnlyOwnOrders() throws Exception {
        Order own = new Order();
        own.setId(1L);
        own.setUserId(2L);
        when(orderService.getByUserId(2L)).thenReturn(Collections.singletonList(own));

        mockMvc.perform(get("/api/orders").header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value(2));

        verify(orderService).getByUserId(2L);
    }

    /**
     * 管理员 GET /api/orders → 查全部订单（getAll）。
     */
    @Test
    void getAll_asAdmin_returnsAllOrders() throws Exception {
        Order o1 = new Order();
        o1.setId(1L);
        o1.setUserId(2L);
        Order o2 = new Order();
        o2.setId(2L);
        o2.setUserId(3L);
        when(orderService.getAll()).thenReturn(Arrays.asList(o1, o2));

        mockMvc.perform(get("/api/orders").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        verify(orderService).getAll();
    }

    // ==================== 越权防护：水平越权 ====================

    /**
     * 普通用户访问自己的订单详情 → 200。
     */
    @Test
    void getById_ownOrder_returnsOk() throws Exception {
        Order own = new Order();
        own.setId(1L);
        own.setUserId(2L);
        when(orderService.getById(1L)).thenReturn(own);

        mockMvc.perform(get("/api/orders/1").header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(2));
    }

    /**
     * 普通用户访问他人的订单（userId=99）→ assertOrderAccess 抛 403。
     * 这是【水平越权防护】的核心用例：用户 A 不能通过猜 id 看用户 B 的订单。
     */
    @Test
    void getById_otherUserOrder_returns403() throws Exception {
        Order others = new Order();
        others.setId(1L);
        others.setUserId(99L);
        when(orderService.getById(1L)).thenReturn(others);

        mockMvc.perform(get("/api/orders/1").header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(Result.FORBIDDEN_CODE));
    }

    /**
     * 订单不存在（getById 返回 null）→ assertOrderAccess 抛 404。
     */
    @Test
    void getById_nonExistingOrder_returns404() throws Exception {
        when(orderService.getById(1L)).thenReturn(null);

        mockMvc.perform(get("/api/orders/1").header("Authorization", "Bearer user-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(Result.NOT_FOUND_CODE));
    }

    /**
     * GET /api/orders/user/{userId}：普通用户查他人 → 403。
     */
    @Test
    void getByUserId_asUser_otherUser_returns403() throws Exception {
        mockMvc.perform(get("/api/orders/user/99").header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(Result.FORBIDDEN_CODE));
    }

    /**
     * GET /api/orders/user/{userId}：普通用户查自己 → 200。
     */
    @Test
    void getByUserId_asUser_own_returnsOk() throws Exception {
        Order own = new Order();
        own.setId(1L);
        own.setUserId(2L);
        when(orderService.getByUserId(2L)).thenReturn(Collections.singletonList(own));

        mockMvc.perform(get("/api/orders/user/2").header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value(2));
    }

    /**
     * GET /api/orders/user/{userId}：管理员查任意用户 → 200（管理员不受限）。
     */
    @Test
    void getByUserId_asAdmin_otherUser_returnsOk() throws Exception {
        Order others = new Order();
        others.setId(1L);
        others.setUserId(99L);
        when(orderService.getByUserId(99L)).thenReturn(Collections.singletonList(others));

        mockMvc.perform(get("/api/orders/user/99").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value(99));
    }

    // ==================== 创建订单：userId 强制绑定 ====================

    /**
     * 普通用户下单时即便前端伪造 order.userId=99，控制器也必须覆盖为当前登录用户 2。
     * 用 ArgumentCaptor 捕获实际传给 Service 的 Order，断言其 userId == 2（防越权/防刷单）。
     */
    @Test
    void save_asUser_forcesUserIdToCurrentUser() throws Exception {
        String body = "{\"order\":{\"userId\":99,\"address\":\"a\",\"phone\":\"13800138000\",\"receiver\":\"r\"},"
                + "\"orderItems\":[{\"productId\":1,\"quantity\":2}]}";

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        var orderCaptor = forClass(Order.class);
        verify(orderService).save(orderCaptor.capture(), any());
        assertEquals(2L, orderCaptor.getValue().getUserId(), "普通用户的 userId 必须被强制覆盖为当前登录用户");
    }

    /**
     * 管理员代他人下单：保留前端传入的 userId=99。
     */
    @Test
    void save_asAdmin_keepsProvidedUserId() throws Exception {
        String body = "{\"order\":{\"userId\":99,\"address\":\"a\",\"phone\":\"13800138000\",\"receiver\":\"r\"},"
                + "\"orderItems\":[{\"productId\":1,\"quantity\":1}]}";

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        var orderCaptor = forClass(Order.class);
        verify(orderService).save(orderCaptor.capture(), any());
        assertEquals(99L, orderCaptor.getValue().getUserId(), "管理员可代他人下单，保留传入的 userId");
    }

    // ==================== 状态机 + 幂等：普通用户确认收货 ====================

    /**
     * 普通用户确认自己的【已支付】订单（status 1→2）→ 200，并调用 orderService.update。
     */
    @Test
    void update_asUser_confirmReceipt_paidOwnOrder_returnsOk() throws Exception {
        Order paid = new Order();
        paid.setId(1L);
        paid.setUserId(2L);
        paid.setStatus(1);
        when(orderService.getById(1L)).thenReturn(paid);

        mockMvc.perform(put("/api/orders")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"targetStatus\":2}"))
                .andExpect(status().isOk());

        // [FIX-1] 普通用户确认收货只调用 updateStatusByOrderNo，不再调用全字段 update
        verify(orderService).updateStatusByOrderNo(any(), eq(2));
    }

    /**
     * 普通用户把状态改成非 2（如 3）→ confirmReceipt 抛 403（用户只能确认收货）。
     */
    @Test
    void update_asUser_nonReceiptStatus_returns403() throws Exception {
        Order paid = new Order();
        paid.setId(1L);
        paid.setUserId(2L);
        paid.setStatus(1);
        when(orderService.getById(1L)).thenReturn(paid);

        mockMvc.perform(put("/api/orders")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"targetStatus\":3}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(Result.FORBIDDEN_CODE));
    }

    /**
     * 普通用户对已【未支付】订单（status=0）确认收货 → 当前状态前置不满足，抛 409。
     */
    @Test
    void update_asUser_notPaidOrder_returns409() throws Exception {
        Order unpaid = new Order();
        unpaid.setId(1L);
        unpaid.setUserId(2L);
        unpaid.setStatus(0);
        when(orderService.getById(1L)).thenReturn(unpaid);

        mockMvc.perform(put("/api/orders")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"targetStatus\":2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(Result.CONFLICT_CODE));
    }

    /**
     * 普通用户重复确认收货（订单已是 status=2）→ 幂等，返回 200 且不再调用 update。
     */
    @Test
    void update_asUser_alreadyReceived_isIdempotent() throws Exception {
        Order done = new Order();
        done.setId(1L);
        done.setUserId(2L);
        done.setStatus(2);
        when(orderService.getById(1L)).thenReturn(done);

        mockMvc.perform(put("/api/orders")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"targetStatus\":2}"))
                .andExpect(status().isOk());

        // [FIX-1] 已收货订单确认收货：状态机允许(2->2)幂等，调用 updateStatusByOrderNo，不再调用全字段 update
        verify(orderService, never()).update(any());
        verify(orderService).updateStatusByOrderNo(any(), eq(2));
    }

    /**
     * 管理员更新订单：走 applyAdminUpdate 分支，仅可改地址等白名单字段与合法状态流转，调用 orderService.update。
     */
    @Test
    void update_asAdmin_mergesAndUpdates() throws Exception {
        Order existing = new Order();
        existing.setId(1L);
        existing.setUserId(2L);
        existing.setStatus(0);
        existing.setAddress("old");
        when(orderService.getById(1L)).thenReturn(existing);

        mockMvc.perform(put("/api/orders")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"address\":\"new address\"}"))
                .andExpect(status().isOk());

        verify(orderService).update(any(Order.class));
    }

    // ==================== 删除：越权防护 ====================

    /**
     * 普通用户删除他人订单（userId=99）→ 403，且不调用 orderService.delete。
     */
    @Test
    void delete_asUser_otherOrder_returns403() throws Exception {
        Order others = new Order();
        others.setId(1L);
        others.setUserId(99L);
        when(orderService.getById(1L)).thenReturn(others);

        mockMvc.perform(delete("/api/orders/1").header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(Result.FORBIDDEN_CODE));

        verify(orderService, never()).delete(any(Long.class));
    }
}
