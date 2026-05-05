package com.community.platform.exception;

import com.community.platform.common.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 处理权限不足异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<?> handleAccessDeniedException(AccessDeniedException e, HttpServletResponse response) {
        response.setStatus(403);
        return Result.error(403, "没有权限访问该资源");
    }
    
    /**
     * 处理未认证异常
     */
    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public Result<?> handleAuthenticationException(AuthenticationCredentialsNotFoundException e, HttpServletResponse response) {
        response.setStatus(401);
        return Result.error(401, "未登录或登录已过期，请重新登录");
    }
    
    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e, HttpServletResponse response) {
        e.printStackTrace();  // 打印堆栈信息便于调试
        return Result.error("服务器错误: " + e.getMessage());
    }
}
