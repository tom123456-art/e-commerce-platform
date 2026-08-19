package com.example.ecommerce.common;

/**
 * 库存不足业务异常
 */
public class StockNotEnoughException extends BusinessException {

    public StockNotEnoughException() {
        super(Result.CONFLICT_CODE, "库存不足");
    }
}