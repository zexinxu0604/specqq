package com.specqq.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 登录响应DTO
 *
 * @author Chatbot Router System
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /**
     * JWT访问令牌
     */
    private String accessToken;

    /**
     * 令牌类型（Bearer）
     */
    private String tokenType = "Bearer";

    /**
     * 过期时间（秒）
     */
    private Long expiresIn;

    /**
     * 用户信息
     */
    private UserInfo userInfo;

    /**
     * 登录时间
     */
    private LocalDateTime loginTime;

    public LoginResponse(String accessToken, Long expiresIn, UserInfo userInfo) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.userInfo = userInfo;
        this.loginTime = LocalDateTime.now();
    }
}
