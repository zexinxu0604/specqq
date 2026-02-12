package com.specqq.chatbot.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.List;

/**
 * 默认规则配置 DTO
 * 用于配置新群组自动绑定的默认规则
 *
 * @author Claude Code
 * @since 2026-02-12
 */
@Builder
public record DefaultRuleConfigDTO(
        @NotEmpty(message = "规则ID列表不能为空")
        List<Long> ruleIds
) {
    /**
     * 创建默认配置
     *
     * @param ruleIds 规则ID列表
     * @return 默认规则配置
     */
    public static DefaultRuleConfigDTO of(List<Long> ruleIds) {
        return new DefaultRuleConfigDTO(ruleIds);
    }

    /**
     * 判断是否包含指定规则
     *
     * @param ruleId 规则ID
     * @return true 如果包含该规则
     */
    public boolean containsRule(Long ruleId) {
        return ruleIds != null && ruleIds.contains(ruleId);
    }

    /**
     * 获取规则数量
     *
     * @return 规则数量
     */
    public int getRuleCount() {
        return ruleIds == null ? 0 : ruleIds.size();
    }
}
