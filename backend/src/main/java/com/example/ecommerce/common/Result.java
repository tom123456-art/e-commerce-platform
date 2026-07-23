package com.example.ecommerce.common;

import java.io.Serializable;

/**
 * 统一响应封装类。
 * 所有 API 的返回格式统一为：
 * {
 *   "success": true,        // 业务是否成功
 *   "code": 200,            // 状态码
 *   "message": "success",   // 结果描述
 *   "data": { ... },        // 业务数据（泛型）
 *   "timestamp": 1718700000000 // 响应时间戳
 * }
 * @param <T> data 字段的类型，由具体接口决定
 */
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    // ========== 状态码常量（与 HTTP 状态语义一致） ==========
    public static final int SUCCESS_CODE = 200;      // 成功
    public static final int BAD_REQUEST_CODE = 400; // 参数错误
    public static final int UNAUTHORIZED_CODE = 401;// 未登录
    public static final int FORBIDDEN_CODE = 403;   // 无权限
    public static final int NOT_FOUND_CODE = 404;   // 资源不存在
    public static final int CONFLICT_CODE = 409;     // 数据冲突
    public static final int ERROR_CODE = 500;       // 服务器错误

    // ========== 实例字段 ==========
    private boolean success;    // 业务是否成功
    private int code;           // 业务状态码
    private String message;     // 结果描述
    private T data;             // 业务数据
    private long timestamp;     // 响应时间戳

    /** 默认构造方法，自动设置时间戳 */
    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    // ========== 静态工厂方法 ==========
    /**
     * 创建成功响应（携带数据）。
     * 用法: Result.success(user) 或 Result.success(productList)
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setSuccess(true);
        result.setCode(SUCCESS_CODE);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    /** 创建成功响应（不携带数据），用于删除等操作 */
    public static Result<Void> success() {
        return success(null);
    }

    /**
     * 创建失败响应。
     * 通常由 GlobalExceptionHandler 自动调用，开发者很少直接使用。
     */
    public static <T> Result<T> failure(int code, String message) {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    // ========== Getter / Setter ==========
    // Jackson 序列化时需要通过 getter 读取字段值
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}