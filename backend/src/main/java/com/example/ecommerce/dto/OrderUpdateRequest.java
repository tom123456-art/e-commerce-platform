package com.example.ecommerce.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * [FIX-5] 订单更新请求 DTO。
 * 用专用 DTO 替代直接接收 Order 实体（防止 Mass Assignment 越权字段注入）。
 * 仅暴露允许被修改的字段：
 * - id：订单主键（必填）
 * - targetStatus：目标状态（可选；普通用户仅允许确认收货=2）
 * - address / phone / receiver：收货信息（可选，仅管理员可改）
 * 订单号、金额、userId 等敏感字段不在此 DTO 中，绝不允许通过更新接口修改。
 */
@Data
public class OrderUpdateRequest {
    /** 订单主键，必填 */
    @NotNull(message = "订单ID不能为空")
    private Long id;
    /** 目标状态，可选 */
    private Integer targetStatus;
    /** 收货地址，可选 */
    private String address;
    /** 联系电话，可选 */
    private String phone;
    /** 收货人，可选 */
    private String receiver;
}
