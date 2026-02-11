package com.specqq.chatbot.vo;

import com.specqq.chatbot.common.enums.MatchType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 规则列表 VO
 *
 * <p>用于规则列表展示，包含核心信息和简要统计</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "规则列表项")
public class RuleListVO {

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
     * 匹配模式 (简化显示)
     */
    @Schema(description = "匹配模式", example = "你好|hello|hi")
    private String pattern;

    /**
     * 优先级
     */
    @Schema(description = "优先级 (1-1000, 数字越小优先级越高)", example = "100")
    private Integer priority;

    /**
     * 是否启用
     */
    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;

    /**
     * 是否配置了策略
     */
    @Schema(description = "是否配置了策略", example = "true")
    private Boolean hasPolicy;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2026-02-11T10:30:00")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间", example = "2026-02-11T15:45:00")
    private LocalDateTime updateTime;

    /**
     * 最后触发时间
     */
    @Schema(description = "最后触发时间", example = "2026-02-11T15:45:00")
    private LocalDateTime lastTriggeredAt;

    /**
     * 触发次数
     */
    @Schema(description = "触发次数", example = "1250")
    private Long triggerCount;
}
