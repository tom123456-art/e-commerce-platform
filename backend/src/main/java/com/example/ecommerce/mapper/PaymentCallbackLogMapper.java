package com.example.ecommerce.mapper;

import com.example.ecommerce.entity.PaymentCallbackLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付回调日志数据访问接口。
 * <p>
 * 作用：
 * - 审计追踪：记录每次回调的原始数据，便于问题排查
 * - 幂等性保障：通过日志判断回调是否已处理，防止重复处理
 * - 数据一致性：记录验证和处理结果，确保支付状态与订单状态一致
 */
@Mapper
public interface PaymentCallbackLogMapper {

    /**
     * 插入支付回调日志（收到回调时立即记录原始数据）。
     * 记录订单号、交易号、交易状态、原始回调数据（raw_payload）等。
     */
    int insert(PaymentCallbackLog log);

    /**
     * 更新回调日志的处理结果（处理完成后更新）。
     * 更新 verified（是否验签通过）、processed（是否已处理）、success（是否成功）、error_message。
     */
    int updateResult(PaymentCallbackLog log);
}