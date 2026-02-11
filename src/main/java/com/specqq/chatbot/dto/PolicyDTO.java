package com.specqq.chatbot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 策略配置 DTO
 *
 * <p>独立的策略配置对象，可用于单独更新策略</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "策略配置")
public class PolicyDTO {

    /**
     * Scope 策略
     */
    @Schema(description = "作用域 (USER/GROUP/GLOBAL)", example = "USER")
    private String scope = "USER";

    @Schema(description = "白名单 (用户ID或群ID列表)", example = "[\"123456\", \"789012\"]")
    private List<String> whitelist;

    @Schema(description = "黑名单 (用户ID或群ID列表)", example = "[\"111111\", \"222222\"]")
    private List<String> blacklist;

    /**
     * Rate Limit 策略
     */
    @Schema(description = "是否启用限流", example = "true")
    private Boolean rateLimitEnabled = false;

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
    private Boolean timeWindowEnabled = false;

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
    private Boolean roleEnabled = false;

    @Schema(description = "允许的角色列表", example = "[\"owner\", \"admin\"]")
    private List<String> allowedRoles;

    /**
     * Cooldown 策略
     */
    @Schema(description = "是否启用冷却", example = "true")
    private Boolean cooldownEnabled = false;

    @Min(value = 1, message = "冷却时间必须大于0秒")
    @Schema(description = "冷却时间 (秒)", example = "60")
    private Integer cooldownSeconds;
}
