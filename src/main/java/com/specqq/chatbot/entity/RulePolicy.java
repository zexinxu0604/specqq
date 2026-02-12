package com.specqq.chatbot.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 规则策略实体类
 *
 * @author Claude Code
 * @since 2026-02-11
 * @tableName rule_policy
 */
@Data
@TableName(value = "rule_policy", autoResultMap = true)
public class RulePolicy {

    /**
     * 策略 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 规则 ID（外键）
     */
    @TableField("rule_id")
    private Long ruleId;

    // ============================================================
    // Scope 策略
    // ============================================================

    /**
     * 作用域: USER, GROUP, GLOBAL
     */
    @TableField("scope")
    private String scope;

    /**
     * 白名单（用户 ID 或群组 ID 列表）
     */
    @TableField(value = "whitelist", typeHandler = JacksonTypeHandler.class)
    private List<String> whitelist;

    /**
     * 黑名单（用户 ID 或群组 ID 列表）
     */
    @TableField(value = "blacklist", typeHandler = JacksonTypeHandler.class)
    private List<String> blacklist;

    // ============================================================
    // Rate Limit 策略
    // ============================================================

    /**
     * 是否启用限流
     */
    @TableField("rate_limit_enabled")
    private Boolean rateLimitEnabled;

    /**
     * 窗口内最大请求数
     */
    @TableField("rate_limit_max_requests")
    private Integer rateLimitMaxRequests;

    /**
     * 时间窗口（秒）
     */
    @TableField("rate_limit_window_seconds")
    private Integer rateLimitWindowSeconds;

    // ============================================================
    // Time Window 策略
    // ============================================================

    /**
     * 是否启用时间窗口
     */
    @TableField("time_window_enabled")
    private Boolean timeWindowEnabled;

    /**
     * 允许开始时间
     */
    @TableField("time_window_start")
    private LocalTime timeWindowStart;

    /**
     * 允许结束时间
     */
    @TableField("time_window_end")
    private LocalTime timeWindowEnd;

    /**
     * 允许星期几（逗号分隔: 1,2,3,4,5）
     */
    @TableField("time_window_weekdays")
    private String timeWindowWeekdays;

    // ============================================================
    // Role 策略
    // ============================================================

    /**
     * 是否启用角色检查
     */
    @TableField("role_enabled")
    private Boolean roleEnabled;

    /**
     * 允许的角色列表
     */
    @TableField(value = "allowed_roles", typeHandler = JacksonTypeHandler.class)
    private List<String> allowedRoles;

    // ============================================================
    // Cooldown 策略
    // ============================================================

    /**
     * 是否启用冷却
     */
    @TableField("cooldown_enabled")
    private Boolean cooldownEnabled;

    /**
     * 冷却时间（秒）
     */
    @TableField("cooldown_seconds")
    private Integer cooldownSeconds;

    // ============================================================
    // 审计字段
    // ============================================================

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
