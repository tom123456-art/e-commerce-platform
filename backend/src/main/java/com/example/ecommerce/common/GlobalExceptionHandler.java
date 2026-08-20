package com.example.ecommerce.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 *
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody
 * 作用：拦截所有 Controller 中抛出的异常，转换为统一的 Result 格式返回。
 * <p>
 * 处理流程：
 * Controller 正常返回 → Spring 自动序列化为 JSON
 * Controller 抛出异常 → GlobalExceptionHandler 捕获 → 转换为 Result.failure() → 返回 JSON
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常（最常用）。
     * Service 层抛出 BusinessException 时自动触发。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException ex) {
        return ResponseEntity.status(toStatus(ex.getCode()))
                .body(Result.failure(ex.getCode(), ex.getMessage()));
    }

    /**
     * 处理请求参数相关的异常（统一映射到 400）。
     * 包括：参数校验失败、JSON 格式错误、缺少必需参数等。
     */
    @ExceptionHandler({
            IllegalArgumentException.class,
            BindException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentNotValidException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<Result<Void>> handleBadRequest(Exception ex) {
        return ResponseEntity.badRequest()
                .body(Result.failure(Result.BAD_REQUEST_CODE, extractMessage(ex)));
    }

    /**
     * 处理认证异常（未登录）。
     * Spring Security 在用户未携带 Token 或 Token 过期时抛出。
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Void>> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.failure(Result.UNAUTHORIZED_CODE, "Please login first"));
    }

    /**
     * 处理授权异常（无权限）。
     * 普通用户访问管理员接口时触发。
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.failure(Result.FORBIDDEN_CODE, "No permission"));
    }

    /**
     * 处理数据库数据完整性约束异常。
     * 如：唯一索引冲突（重复注册）、外键约束冲突等。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Result.failure(Result.CONFLICT_CODE, "Data conflict or duplicate record"));
    }

    /**
     * 处理静态资源找不到异常。
     * 第0章没有 Controller，访问根路径会触发此异常，返回 404 而非 500。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.failure(Result.NOT_FOUND_CODE, "Resource not found"));
    }

    /**
     * 兜底异常处理器 - 捕获所有未被上述处理器捕获的异常。
     * 安全原则：永远不要把 Java 堆栈信息返回给前端！
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception ex) {
        String errorMessage = ex.getMessage();
        // 检测是否是 XSS 攻击内容
        if (XssUtils.containsXss(errorMessage)) {
            log.warn("[安全告警] 检测到疑似XSS攻击内容");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Result.failure(Result.BAD_REQUEST_CODE, "请求内容包含非法字符"));
        }

        // 二次兜底，防止异常分发错乱
        if (ex instanceof BusinessException be) {
            return ResponseEntity.badRequest()
                    .body(Result.failure(be.getCode(), be.getMessage()));
        }

        // 未知异常：记录详细日志，但只返回模糊的错误信息
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.failure(Result.ERROR_CODE, "System error, please try again later"));
    }

    /**
     * 将业务状态码转换为 HTTP 状态码
     */
    private HttpStatus toStatus(int code) {
        HttpStatus status = HttpStatus.resolve(code);
        return status == null ? HttpStatus.BAD_REQUEST : status;
    }

    /**
     * 从参数异常中提取友好的错误信息
     */
    private String extractMessage(Exception ex) {
        if (ex instanceof MethodArgumentNotValidException exception) {
            return exception.getBindingResult().getFieldErrors().stream()
                    .map(fieldError -> fieldError.getDefaultMessage())
                    .filter(message -> message != null && !message.trim().isEmpty())
                    .collect(Collectors.joining("; "));
        }
        if (ex instanceof BindException exception) {
            return exception.getBindingResult().getFieldErrors().stream()
                    .map(fieldError -> fieldError.getDefaultMessage())
                    .filter(message -> message != null && !message.trim().isEmpty())
                    .collect(Collectors.joining("; "));
        }
        return ex.getMessage();
    }
}