package com.specqq.chatbot.dto;

import com.specqq.chatbot.common.enums.MatchType;
import com.specqq.chatbot.common.enums.OnErrorPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 规则更新 DTO
 *
 * <p>用于更新现有规则，所有字段可选（未提供的字段保持原值）</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Data
@Schema(description = "规则更新请求")
public class RuleUpdateDTO {

    /**
     * 规则名称
     */
    @Size(max = 100, message = "规则名称长度不能超过100个字符")
    @Schema(description = "规则名称", example = "问候语规则")
    private String ruleName;

    /**
     * 规则描述
     */
    @Size(max = 500, message = "规则描述长度不能超过500个字符")
    @Schema(description = "规则描述", example = "检测用户问候消息并自动回复")
    private String description;

    /**
     * 匹配类型
     */
    @Schema(description = "匹配类型", example = "CONTAINS")
    private MatchType matchType;

    /**
     * 匹配模式
     */
    @Size(max = 500, message = "匹配模式长度不能超过500个字符")
    @Schema(description = "匹配模式", example = "你好|hello|hi")
    private String pattern;

    /**
     * 优先级 (1-1000, 数字越小优先级越高)
     */
    @Min(value = 1, message = "优先级最小值为1")
    @Max(value = 1000, message = "优先级最大值为1000")
    @Schema(description = "优先级 (1-1000, 数字越小优先级越高)", example = "100")
    private Integer priority;

    /**
     * Handler 配置 (JSON 字符串)
     */
    @Schema(description = "Handler 配置 (JSON 格式)", example = "{\"handlerType\":\"TEXT_REPLY\",\"params\":{\"content\":\"你好呀！\"}}")
    private String handlerConfig;

    /**
     * 错误处理策略
     */
    @Schema(description = "错误处理策略", example = "LOG_ONLY")
    private OnErrorPolicy onErrorPolicy;

    /**
     * 是否启用
     */
    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;

    /**
     * 策略配置
     */
    @Valid
    @Schema(description = "策略配置")
    private PolicyDTO policy;

    /**
     * 策略配置 DTO
     */
    @Data
    @Schema(description = "策略配置")
    public static class PolicyDTO {

        /**
         * Scope 策略
         */
        @Schema(description = "作用域 (USER/GROUP/GLOBAL)", example = "USER")
        private String scope;

        @Schema(description = "白名单 (用户ID或群ID列表)", example = "[\"123456\", \"789012\"]")
        private java.util.List<String> whitelist;

        @Schema(description = "黑名单 (用户ID或群ID列表)", example = "[\"111111\", \"222222\"]")
        private java.util.List<String> blacklist;

        /**
         * Rate Limit 策略
         */
        @Schema(description = "是否启用限流", example = "true")
        private Boolean rateLimitEnabled;

        @Min(value = 1, message = "最大请求数必须大于0")
        @Schema(description = "时间窗口内最大请求数", example = "10")
        private Integer rateLimitMaxRequests;

        @Min(value = 1, message = "时间窗口必须大于0秒")
        @Schema(description = "时间窗口 (秒)", example = "60")
        private Integer rateLimitWindowSeconds;

        /**
         * Time Window 策略
         */
        @Schema(description = "是否启用时间窗口", example = "false")
        private Boolean timeWindowEnabled;

        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$", message = "时间格式必须为 HH:mm:ss")
        @Schema(description = "时间窗口开始时间", example = "09:00:00")
        private String timeWindowStart;

        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$", message = "时间格式必须为 HH:mm:ss")
        @Schema(description = "时间窗口结束时间", example = "18:00:00")
        private String timeWindowEnd;

        @Pattern(regexp = "^[1-7](,[1-7])*$", message = "工作日格式错误，应为逗号分隔的1-7数字")
        @Schema(description = "工作日 (1-7, 逗号分隔)", example = "1,2,3,4,5")
        private String timeWindowWeekdays;

        /**
         * Role 策略
         */
        @Schema(description = "是否启用角色限制", example = "false")
        private Boolean roleEnabled;

        @Schema(description = "允许的角色列表", example = "[\"owner\", \"admin\"]")
        private java.util.List<String> allowedRoles;

        /**
         * Cooldown 策略
         */
        @Schema(description = "是否启用冷却", example = "true")
        private Boolean cooldownEnabled;

        @Min(value = 1, message = "冷却时间必须大于0秒")
        @Schema(description = "冷却时间 (秒)", example = "60")
        private Integer cooldownSeconds;
    }
}
