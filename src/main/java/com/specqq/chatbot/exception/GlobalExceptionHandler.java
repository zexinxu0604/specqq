package com.specqq.chatbot.exception;

import com.specqq.chatbot.common.Result;
import com.specqq.chatbot.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 *
 * @author Chatbot Router System
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数校验异常 (400)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Map<String, String>>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("参数校验失败: {}", errors);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Result.error(ResultCode.BAD_REQUEST, "参数校验失败", errors));
    }

    /**
     * 处理绑定异常 (400)
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Map<String, String>>> handleBindException(BindException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("参数绑定失败: {}", errors);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Result.error(ResultCode.BAD_REQUEST, "参数绑定失败", errors));
    }

    /**
     * 处理缺少请求参数异常 (400)
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParameterException(MissingServletRequestParameterException ex) {
        String message = String.format("缺少必需参数: %s", ex.getParameterName());
        log.warn(message);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Result.error(ResultCode.BAD_REQUEST, message));
    }

    /**
     * 处理参数类型不匹配异常 (400)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        String message = String.format("参数类型错误: %s 应为 %s 类型",
            ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "未知");
        log.warn(message);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Result.error(ResultCode.BAD_REQUEST, message));
    }

    /**
     * 处理非法参数异常 (400)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("非法参数: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Result.error(ResultCode.BAD_REQUEST, ex.getMessage()));
    }

    /**
     * 处理认证异常 (401)
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Void>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("认证失败: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(Result.error(ResultCode.UNAUTHORIZED, "认证失败: " + ex.getMessage()));
    }

    /**
     * 处理凭证错误异常 (401)
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Result<Void>> handleBadCredentialsException(BadCredentialsException ex) {
        log.warn("用户名或密码错误");
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(Result.error(ResultCode.AUTH_FAILED, "用户名或密码错误"));
    }

    /**
     * 处理权限不足异常 (403)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("权限不足: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(Result.error(ResultCode.FORBIDDEN, "权限不足"));
    }

    /**
     * 处理资源未找到异常 (404)
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Result<Void>> handleNotFoundException(NoHandlerFoundException ex) {
        log.warn("资源未找到: {}", ex.getRequestURL());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(Result.error(ResultCode.NOT_FOUND, "请求的资源不存在"));
    }

    /**
     * 处理非法状态异常 (409)
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Result<Void>> handleIllegalStateException(IllegalStateException ex) {
        log.warn("非法状态: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(Result.error(ResultCode.CONFLICT, ex.getMessage()));
    }

    /**
     * 处理业务异常 (429 - Too Many Requests for rate limiting, 400 for other business errors)
     *
     * <p>T116: Handle BusinessException for rate limiting and other business logic violations.</p>
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException ex) {
        log.warn("业务异常: {}", ex.getMessage());

        // If it's a rate limit exception, return 429
        if (ex.getMessage() != null && ex.getMessage().contains("Rate limit exceeded")) {
            return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Result.error(ResultCode.TOO_MANY_REQUESTS, ex.getMessage()));
        }

        // For other business exceptions, return 400
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Result.error(ResultCode.BAD_REQUEST, ex.getMessage()));
    }

    /**
     * 处理运行时异常 (500)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Result<Void>> handleRuntimeException(RuntimeException ex) {
        log.error("运行时异常", ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Result.error(ResultCode.INTERNAL_ERROR, "服务器内部错误: " + ex.getMessage()));
    }

    /**
     * 处理其他未捕获异常 (500)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception ex) {
        log.error("未处理的异常", ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Result.error(ResultCode.INTERNAL_ERROR, "服务器内部错误"));
    }
}
