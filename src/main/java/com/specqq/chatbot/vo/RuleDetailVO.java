package com.specqq.chatbot.vo;

import com.specqq.chatbot.common.enums.MatchType;
import com.specqq.chatbot.common.enums.OnErrorPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 规则详情 VO
 *
 * <p>包含完整的规则信息、策略配置和统计数据</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "规则详情")
public class RuleDetailVO {

    /**
     * 规则 ID
     */
    @Schema(description = "规则 ID", example = "1")
    private Long id;

    /**
     * 规则名称
     */
    @Schema(description = "规则名称", example = "问候语规则")
    private String ruleName;

    /**
     * 规则描述
     */
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
    @Schema(description = "匹配模式", example = "你好|hello|hi")
    private String pattern;

    /**
     * 优先级
     */
    @Schema(description = "优先级 (1-1000, 数字越小优先级越高)", example = "100")
    private Integer priority;

    /**
     * Handler 配置
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
    @Schema(description = "策略配置")
    private PolicyVO policy;

    /**
     * 创建人
     */
    @Schema(description = "创建人", example = "admin")
    private String createBy;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2026-02-11T10:30:00")
    private LocalDateTime createTime;

    /**
     * 更新人
     */
    @Schema(description = "更新人", example = "admin")
    private String updateBy;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间", example = "2026-02-11T15:45:00")
    private LocalDateTime updateTime;

    /**
     * 统计信息
     */
    @Schema(description = "统计信息")
    private StatisticsVO statistics;

    /**
     * 策略配置 VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "策略配置")
    public static class PolicyVO {

        /**
         * Scope 策略
         */
        @Schema(description = "作用域 (USER/GROUP/GLOBAL)", example = "USER")
        private String scope;

        @Schema(description = "白名单 (用户ID或群ID列表)", example = "[\"123456\", \"789012\"]")
        private List<String> whitelist;

        @Schema(description = "黑名单 (用户ID或群ID列表)", example = "[\"111111\", \"222222\"]")
        private List<String> blacklist;

        /**
         * Rate Limit 策略
         */
        @Schema(description = "是否启用限流", example = "true")
        private Boolean rateLimitEnabled;

        @Schema(description = "时间窗口内最大请求数", example = "10")
        private Integer rateLimitMaxRequests;

        @Schema(description = "时间窗口 (秒)", example = "60")
        private Integer rateLimitWindowSeconds;

        /**
         * Time Window 策略
         */
        @Schema(description = "是否启用时间窗口", example = "false")
        private Boolean timeWindowEnabled;

        @Schema(description = "时间窗口开始时间", example = "09:00:00")
        private String timeWindowStart;

        @Schema(description = "时间窗口结束时间", example = "18:00:00")
        private String timeWindowEnd;

        @Schema(description = "工作日 (1-7, 逗号分隔)", example = "1,2,3,4,5")
        private String timeWindowWeekdays;

        /**
         * Role 策略
         */
        @Schema(description = "是否启用角色限制", example = "false")
        private Boolean roleEnabled;

        @Schema(description = "允许的角色列表", example = "[\"owner\", \"admin\"]")
        private List<String> allowedRoles;

        /**
         * Cooldown 策略
         */
        @Schema(description = "是否启用冷却", example = "true")
        private Boolean cooldownEnabled;

        @Schema(description = "冷却时间 (秒)", example = "60")
        private Integer cooldownSeconds;
    }

    /**
     * 统计信息 VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "统计信息")
    public static class StatisticsVO {

        @Schema(description = "总触发次数", example = "1250")
        private Long totalTriggers;

        @Schema(description = "成功执行次数", example = "1200")
        private Long successCount;

        @Schema(description = "失败次数", example = "50")
        private Long failureCount;

        @Schema(description = "平均响应时间 (毫秒)", example = "125")
        private Long avgResponseTime;

        @Schema(description = "最后触发时间", example = "2026-02-11T15:45:00")
        private LocalDateTime lastTriggeredAt;
    }
}
