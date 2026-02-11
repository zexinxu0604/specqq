package com.specqq.chatbot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Rate Limit 窗口记录实体类
 *
 * @author Claude Code
 * @since 2026-02-11
 * @tableName rate_limit_window
 */
@Data
@TableName(value = "rate_limit_window")
public class RateLimitWindow {

    /**
     * 记录 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 策略 ID（外键）
     */
    @TableField("policy_id")
    private Long policyId;

    /**
     * 作用域类型: USER, GROUP, GLOBAL
     */
    @TableField("scope_type")
    private String scopeType;

    /**
     * 作用域 ID（用户 ID、群组 ID 或 global）
     */
    @TableField("scope_id")
    private String scopeId;

    /**
     * 窗口起始时间
     */
    @TableField("window_start")
    private LocalDateTime windowStart;

    /**
     * 请求时间戳（毫秒）
     */
    @TableField("request_timestamp")
    private Long requestTimestamp;

    /**
     * 记录过期时间（窗口结束 + 缓冲）
     */
    @TableField("expires_at")
    private LocalDateTime expiresAt;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
