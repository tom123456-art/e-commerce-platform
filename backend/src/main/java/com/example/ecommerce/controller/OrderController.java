package com.example.ecommerce.controller;

import com.example.ecommerce.common.BusinessException;
import com.example.ecommerce.common.OrderStatus;
import com.example.ecommerce.common.Result;
import com.example.ecommerce.dto.OrderDetailResponse;
import com.example.ecommerce.dto.OrderRequest;
import com.example.ecommerce.dto.OrderUpdateRequest;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.security.CustomUserDetails;
import com.example.ecommerce.security.SecurityUtils;
import com.example.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "订单接口", description = "订单创建、查询、状态管理")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 获取订单列表
     * 管理员可以查看全部订单
     * 普通用户只可以查看自己的订单
     *
     * @param authentication
     * @return
     */
    @GetMapping
    public Result<List<Order>> getAll(Authentication authentication) {
        CustomUserDetails user = SecurityUtils.currentUser(authentication);
        return Result.success(
                user.isAdmin() ?
                        orderService.getAll() :
                        orderService.getByUserId(user.getId())
        );
    }

    /**
     * 根据ID获取订单
     */
    @GetMapping("/{id}")
    public Result<Order> getById(@PathVariable Long id, Authentication authentication) {
        Order order = orderService.getById(id);
        // 越权检查，用户只能查看自己的订单，管理员是可以查看所有订单的
        assertOrderAccess(authentication, order);
        return Result.success(order);
    }

    /**
     * 获取订单的详情
     */
    @GetMapping("/{id}/detail")
    public Result<OrderDetailResponse> getDetail(@PathVariable Long id,
                                                 Authentication authentication) {
        // [FIX-B] 清理冗余判断：service 层查不到会直接抛 NOT_FOUND，这里增加显式防御避免返回 success(null)
        OrderDetailResponse detailById = orderService.getDetailById(id);
        if (detailById == null || detailById.getOrder() == null)
            throw new BusinessException(Result.NOT_FOUND_CODE, "订单不存在");
        assertOrderAccess(authentication, detailById.getOrder());
        return Result.success(detailById);
    }

    /**
     * 根据订单号查询
     */
    @GetMapping("/orderNo/{orderNo}")
    public Result<Order> getByOrderNo(@PathVariable String orderNo,
                                      Authentication authentication) {
        Order order = orderService.getByOrderNo(orderNo);
        assertOrderAccess(authentication, order);
        return Result.success(order);
    }

    /**
     * 根据用户ID查询订单信息
     * 管理员可以查看任意用户的订单，普通用户只能查看自己的订单
     */
    @GetMapping("/user/{userId}")
    public Result<List<Order>> getByUserId(@PathVariable Long userId,
                                           Authentication authentication) {
        // 获取当前登录的用户
        CustomUserDetails user = SecurityUtils.currentUser(authentication);
        if (!user.isAdmin() && !user.getId().equals(userId))
            throw new BusinessException(Result.FORBIDDEN_CODE, "无权限访问");
        return Result.success(orderService.getByUserId(userId));
    }

    /**
     * 创建订单
     * POST  /api/orders
     */
    @PostMapping
    // [FIX-C] 开启 Bean Validation，OrderRequest 上的 @NotNull 自动生效
    public Result<OrderDetailResponse> save(@Valid @RequestBody OrderRequest orderRequest,
                                            Authentication authentication) {
        CustomUserDetails user = SecurityUtils.currentUser(authentication);
        if (orderRequest == null || orderRequest.getOrder() == null)
            throw new BusinessException(Result.BAD_REQUEST_CODE, "订单信息不能为空");
        Order order = orderRequest.getOrder();
        if (!user.isAdmin()) {
            // 普通用户强制绑定自己，防止伪造
            order.setUserId(user.getId());
        } else {
            // [FIX-A] 管理员代客下单必须显式指定 userId，缺失即报错，
            // 避免订单被静默落到管理员自己的账号
            if (order.getUserId() == null)
                throw new BusinessException(Result.BAD_REQUEST_CODE, "管理员代客下单必须指定 userId");
        }
        return Result.success(orderService.save(order, orderRequest.getOrderItems()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, Authentication authentication) {
        assertOrderAccess(authentication, orderService.getById(id));
        orderService.delete(id);
        return Result.success();
    }

    // TODO: 订单更新

    /**
     * 更新订单 PUT /api/orders
     * 普通用户只能更新自己的订单，管理员可以更新所有订单
     * [FIX-5] 改用专用 DTO 接收参数，避免直接暴露 Order 实体（Mass Assignment 风险）
     * [FIX-C] 开启 Bean Validation，OrderUpdateRequest 上的 @NotNull 自动生效
     *
     * @param req            订单更新请求
     * @param authentication
     * @return
     */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody OrderUpdateRequest req,
                               Authentication authentication) {
        if (req == null || req.getId() == null)
            throw new BusinessException(Result.BAD_REQUEST_CODE, "订单信息不能为空");
        // 通过订单id查询订单信息
        Order existing = orderService.getById(req.getId());
        // 越权检查，用户只能更新自己的订单，管理员可以更新所有订单
        assertOrderAccess(authentication, existing);
        // 获取当前用户
        CustomUserDetails user = SecurityUtils.currentUser(authentication);
        if (!user.isAdmin()) {
            // [FIX-1] 普通用户仅允许确认收货，且只做一次状态更新
            confirmReceipt(existing, req);
        } else {
            // [FIX-5] 管理员更新：字段白名单 + 状态机校验
            applyAdminUpdate(existing, req);
        }
        return Result.success();
    }

    /**
     * [FIX-1] 普通用户确认收货：
     * - 仅允许 已支付(1) -> 已收货(2)
     * - 通过状态机方法统一校验流转合法性
     * - 只调用一次状态更新（updateStatusByOrderNo），避免被外层再次 update 覆盖回旧状态
     *
     * @param existing 当前订单（数据库中的最新状态）
     * @param req      更新请求
     */
    private void confirmReceipt(Order existing, OrderUpdateRequest req) {
        Integer target = req.getTargetStatus();
        if (target == null || target != OrderStatus.RECEIVED.getCode())
            throw new BusinessException(Result.FORBIDDEN_CODE, "用户只能确认收货");
        // [FIX-5] 状态机校验：确保当前状态为已支付(1) 才允许变更为已收货(2)
        OrderStatus.validateTransition(existing.getStatus(), target);
        // [FIX-1] 仅更新状态，且只执行一次，杜绝二次覆盖导致状态回退
        orderService.updateStatusByOrderNo(existing.getOrderNo(), target);
    }

    /**
     * [FIX-2][FIX-5][FIX-E] 管理员更新：
     * - 金额(totalAmount)、订单号(orderNo)、userId 一律不可变（始终取数据库现有值）
     * - 仅允许修改收货信息(address/phone/receiver) 及合法状态流转
     * - 状态变更必须经过状态机校验（管理员允许额外的取消/退款流转）
     *
     * @param existing 当前订单
     * @param req      更新请求
     */
    private void applyAdminUpdate(Order existing, OrderUpdateRequest req) {
        Order merged = new Order();
        merged.setId(existing.getId());                  // 不可变
        merged.setUserId(existing.getUserId());          // [FIX-2] 不可变
        merged.setOrderNo(existing.getOrderNo());        // [FIX-2] 订单号不可变
        merged.setTotalAmount(existing.getTotalAmount()); // [FIX-2] 金额不可变
        // 默认沿用原状态与收货信息，前端传了才覆盖
        merged.setStatus(existing.getStatus());
        merged.setAddress(req.getAddress() == null ? existing.getAddress() : req.getAddress());
        merged.setPhone(req.getPhone() == null ? existing.getPhone() : req.getPhone());
        merged.setReceiver(req.getReceiver() == null ? existing.getReceiver() : req.getReceiver());
        if (req.getTargetStatus() != null) {
            // [FIX-E] 管理员走宽松状态机，允许取消/退款（已支付 -> 待支付）
            OrderStatus.validateTransition(existing.getStatus(), req.getTargetStatus(), true);
            merged.setStatus(req.getTargetStatus());
        }
        orderService.update(merged);
    }

    /**
     * 越权检查，用户只能访问自己的订单，管理员可以访问所有订单
     *
     * @param authentication
     * @param order
     */
    private void assertOrderAccess(Authentication authentication, Order order) {
        if (order == null)
            throw new BusinessException(Result.NOT_FOUND_CODE, "订单不存在");
        CustomUserDetails user = SecurityUtils.currentUser(authentication);
        if (!user.isAdmin() && !user.getId().equals(order.getUserId()))
            throw new BusinessException(Result.FORBIDDEN_CODE, "无权限访问");
    }

}
