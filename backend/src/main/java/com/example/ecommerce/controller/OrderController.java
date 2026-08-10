package com.example.ecommerce.controller;

import com.example.ecommerce.common.BusinessException;
import com.example.ecommerce.common.Result;
import com.example.ecommerce.dto.OrderDetailResponse;
import com.example.ecommerce.dto.OrderRequest;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.security.CustomUserDetails;
import com.example.ecommerce.security.SecurityUtils;
import com.example.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    public Result<Order> getById(@PathVariable Long id, Authentication authentication){
        Order order = orderService.getById(id);
        // 越权检查，用户智能查看自己的订单，管理员是可以查看所有订单的
        assertOrderAccess(authentication, order);
        return Result.success(order);
    }

    /**
     * 获取订单的详情
     */
    @GetMapping("/{id}/detail")
    public Result<OrderDetailResponse> getDetail(@PathVariable Long id,
                                                 Authentication authentication){
        OrderDetailResponse detailById = orderService.getDetailById(id);
        if (detailById != null)
            assertOrderAccess(authentication, detailById.getOrder());
        return Result.success(detailById);
    }

    /**
     * 根据订单号查询
     */
    @GetMapping("/orderNo/{orderNo}")
    public Result<Order> getByOrderNo(@PathVariable String orderNo,
                                      Authentication authentication){
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
                                           Authentication authentication){
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
    public Result<OrderDetailResponse> save(@RequestBody OrderRequest orderRequest,
                                            Authentication authentication){
        CustomUserDetails user = SecurityUtils.currentUser(authentication);
        if (orderRequest == null || orderRequest.getOrder() == null)
            throw new BusinessException(Result.BAD_REQUEST_CODE, "订单信息不能为空");
        Order order = orderRequest.getOrder();
        if (!user.isAdmin())
            // 强制绑定用户ID，防止伪造
            order.setUserId(user.getId());
        else if (order.getUserId() == null)
            order.setUserId(user.getId());
        return Result.success(orderService.save(order, orderRequest.getOrderItems()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, Authentication authentication){
        assertOrderAccess(authentication,orderService.getById(id));
        orderService.delete(id);
        return Result.success();
    }

    // TODO: 订单更新





    /**
     * 越权检查，用户只能访问自己的订单，管理员可以访问所有订单
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
