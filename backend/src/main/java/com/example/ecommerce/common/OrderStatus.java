package com.example.ecommerce.common;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 订单状态枚举 + 状态机校验。
 * 用于替代散落在各处的魔法数字（0/1/2...），并统一约束状态流转，防止任意跳转。
 */
public enum OrderStatus {
    PENDING_PAYMENT(0, "待支付"),
    PAID(1, "已支付"),
    RECEIVED(2, "已收货"),
    DELETED(-1, "已删除");

    private final int code;
    private final String desc;

    OrderStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    private static final Map<Integer, OrderStatus> MAP = new HashMap<>();
    static {
        for (OrderStatus s : values()) {
            MAP.put(s.code, s);
        }
    }

    public static OrderStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        return MAP.get(code);
    }

    /**
     * [FIX-5] 状态机（普通用户/默认）：允许的流转集合。
     * - 待支付(0) -> 已支付(1)
     * - 已支付(1) -> 已收货(2)
     * 其余流转一律禁止。
     */
    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = new HashMap<>();
    static {
        TRANSITIONS.put(PENDING_PAYMENT, EnumSet.of(PAID));
        TRANSITIONS.put(PAID, EnumSet.of(RECEIVED));
    }

    /**
     * [FIX-E] 状态机（管理员）：在普通流转基础上，额外允许"取消/退款"。
     * - 已支付(1) -> 待支付(0)（管理员取消订单或退款）
     * 其余终态（已收货）不再允许流转。
     */
    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS_ADMIN = new HashMap<>();
    static {
        TRANSITIONS_ADMIN.put(PENDING_PAYMENT, EnumSet.of(PAID));
        TRANSITIONS_ADMIN.put(PAID, EnumSet.of(RECEIVED, PENDING_PAYMENT));
    }

    /**
     * [FIX-5] 校验从 fromCode 变更为 toCode 是否合法。
     * 允许流转到自身（状态不变）；非法流转抛出 BusinessException(CONFLICT)。
     */
    /**
     * [FIX-5] 普通用户的严格状态机校验（等价于 isAdmin=false）。
     */
    public static void validateTransition(Integer fromCode, Integer toCode) {
        validateTransition(fromCode, toCode, false);
    }

    /**
     * [FIX-E] 状态机校验，支持管理员宽松流转。
     * @param isAdmin 是否为管理员（管理员允许额外的取消/退款流转）
     */
    public static void validateTransition(Integer fromCode, Integer toCode, boolean isAdmin) {
        OrderStatus from = fromCode == null ? null : MAP.get(fromCode);
        OrderStatus to = toCode == null ? null : MAP.get(toCode);
        if (to == null) {
            throw new BusinessException(Result.BAD_REQUEST_CODE, "非法的订单状态");
        }
        if (from == null) {
            throw new BusinessException(Result.CONFLICT_CODE, "订单当前状态未知，无法变更");
        }
        if (from == to) {
            return; // 状态不变，允许
        }
        Set<OrderStatus> allowed = (isAdmin ? TRANSITIONS_ADMIN : TRANSITIONS).get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw new BusinessException(Result.CONFLICT_CODE,
                    "订单状态不允许从【" + from.desc + "】变更为【" + to.desc + "】");
        }
    }
}
