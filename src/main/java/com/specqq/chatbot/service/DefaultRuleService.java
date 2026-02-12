package com.specqq.chatbot.service;

import com.specqq.chatbot.dto.DefaultRuleConfigDTO;
import com.specqq.chatbot.entity.GroupChat;
import com.specqq.chatbot.entity.MessageRule;

import java.util.List;

/**
 * 默认规则服务接口
 * 提供默认规则配置管理和自动绑定功能
 *
 * @author Claude Code
 * @since 2026-02-12
 */
public interface DefaultRuleService {

    /**
     * 获取当前默认规则配置
     *
     * @return 默认规则配置
     */
    DefaultRuleConfigDTO getDefaultRuleConfig();

    /**
     * 更新默认规则配置
     *
     * @param config 新的默认规则配置
     */
    void updateDefaultRuleConfig(DefaultRuleConfigDTO config);

    /**
     * 获取默认规则列表
     *
     * @return 规则实体列表
     */
    List<MessageRule> getDefaultRules();

    /**
     * 为新群组自动绑定默认规则
     *
     * @param groupChat 新群组实体
     * @return 绑定的规则数量
     */
    Integer applyDefaultRulesToGroup(GroupChat groupChat);

    /**
     * 批量为群组绑定默认规则
     *
     * @param groupChats 群组列表
     * @return 总共绑定的规则数量
     */
    Integer batchApplyDefaultRules(List<GroupChat> groupChats);

    /**
     * 验证规则ID是否有效
     *
     * @param ruleIds 规则ID列表
     * @return 无效的规则ID列表（空列表表示全部有效）
     */
    List<Long> validateRuleIds(List<Long> ruleIds);

    /**
     * 判断群组是否已绑定默认规则
     *
     * @param groupId 群组ID
     * @return true 如果已绑定默认规则
     */
    Boolean hasDefaultRules(Long groupId);

    /**
     * 获取未绑定默认规则的群组列表
     *
     * @return 群组列表
     */
    List<GroupChat> getGroupsWithoutDefaultRules();
}
