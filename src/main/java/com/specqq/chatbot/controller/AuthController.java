package com.specqq.chatbot.controller;

import com.specqq.chatbot.common.Result;
import com.specqq.chatbot.common.ResultCode;
import com.specqq.chatbot.dto.LoginRequest;
import com.specqq.chatbot.dto.LoginResponse;
import com.specqq.chatbot.dto.UserInfo;
import com.specqq.chatbot.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * @author Chatbot Router System
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "认证管理", description = "用户认证和授权接口")
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "使用用户名和密码登录，返回JWT令牌")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("用户登录请求: username={}", request.getUsername());

        try {
            LoginResponse response = authService.login(request.getUsername(), request.getPassword());
            log.info("用户登录成功: username={}", request.getUsername());
            return Result.success("登录成功", response);
        } catch (IllegalArgumentException e) {
            log.warn("登录失败: username={}, reason={}", request.getUsername(), e.getMessage());
            return Result.error(ResultCode.AUTH_FAILED, e.getMessage());
        }
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "清除当前用户的认证令牌")
    public Result<Void> logout(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null) {
            String username = authService.getUsernameFromToken(token);
            log.info("用户登出: username={}", username);
            authService.logout(token);
        }
        return Result.success("登出成功", null);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/user-info")
    @Operation(summary = "获取用户信息", description = "获取当前登录用户的详细信息")
    public Result<UserInfo> getUserInfo(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            return Result.error(ResultCode.UNAUTHORIZED, "未提供认证令牌");
        }

        try {
            UserInfo userInfo = authService.getUserInfo(token);
            return Result.success(userInfo);
        } catch (IllegalArgumentException e) {
            log.warn("获取用户信息失败: {}", e.getMessage());
            return Result.error(ResultCode.UNAUTHORIZED, e.getMessage());
        }
    }

    /**
     * 刷新令牌
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "使用当前令牌获取新的访问令牌")
    public Result<LoginResponse> refreshToken(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            return Result.error(ResultCode.UNAUTHORIZED, "未提供认证令牌");
        }

        try {
            LoginResponse response = authService.refreshToken(token);
            log.info("令牌刷新成功");
            return Result.success("令牌刷新成功", response);
        } catch (IllegalArgumentException e) {
            log.warn("令牌刷新失败: {}", e.getMessage());
            return Result.error(ResultCode.UNAUTHORIZED, e.getMessage());
        }
    }

    /**
     * 初始化默认管理员账户
     */
    @PostMapping("/init-admin")
    @Operation(summary = "初始化管理员", description = "创建默认管理员账户（仅在无管理员时可用）")
    public Result<Void> initAdmin() {
        log.info("初始化默认管理员账户请求");

        try {
            authService.initDefaultAdmin();
            log.info("默认管理员账户初始化成功");
            return Result.success("管理员账户初始化成功", null);
        } catch (IllegalStateException e) {
            log.warn("管理员账户初始化失败: {}", e.getMessage());
            return Result.error(ResultCode.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    @Operation(summary = "修改密码", description = "修改当前用户的密码")
    public Result<Void> changePassword(
        @Parameter(description = "旧密码") @RequestParam String oldPassword,
        @Parameter(description = "新密码") @RequestParam String newPassword,
        HttpServletRequest request
    ) {
        String token = extractToken(request);
        if (token == null) {
            return Result.error(ResultCode.UNAUTHORIZED, "未提供认证令牌");
        }

        try {
            String username = authService.getUsernameFromToken(token);
            authService.changePassword(username, oldPassword, newPassword);
            log.info("密码修改成功: username={}", username);
            return Result.success("密码修改成功", null);
        } catch (IllegalArgumentException e) {
            log.warn("密码修改失败: {}", e.getMessage());
            return Result.error(ResultCode.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 从请求头中提取JWT令牌
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
