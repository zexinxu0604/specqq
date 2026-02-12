package com.specqq.chatbot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.specqq.chatbot.dto.DefaultRuleConfigDTO;
import com.specqq.chatbot.entity.GroupChat;
import com.specqq.chatbot.entity.GroupRuleConfig;
import com.specqq.chatbot.entity.MessageRule;
import com.specqq.chatbot.mapper.GroupChatMapper;
import com.specqq.chatbot.mapper.GroupRuleConfigMapper;
import com.specqq.chatbot.mapper.MessageRuleMapper;
import com.specqq.chatbot.service.DefaultRuleService;
import com.specqq.chatbot.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 默认规则服务实现
 *
 * @author Claude Code
 * @since 2026-02-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultRuleServiceImpl implements DefaultRuleService {

    private final SystemConfigService systemConfigService;
    private final MessageRuleMapper messageRuleMapper;
    private final GroupRuleConfigMapper groupRuleConfigMapper;
    private final GroupChatMapper groupChatMapper;

    @Override
    public DefaultRuleConfigDTO getDefaultRuleConfig() {
        DefaultRuleConfigDTO config = systemConfigService.getDefaultGroupRules();
        if (config == null || config.ruleIds() == null || config.ruleIds().isEmpty()) {
            log.debug("默认规则配置为空，返回空列表");
            return DefaultRuleConfigDTO.of(Collections.emptyList());
        }
        log.debug("获取默认规则配置: ruleCount={}", config.getRuleCount());
        return config;
    }

    @Override
    @Transactional
    public void updateDefaultRuleConfig(DefaultRuleConfigDTO config) {
        // 验证规则ID是否有效
        List<Long> invalidRuleIds = validateRuleIds(config.ruleIds());
        if (!invalidRuleIds.isEmpty()) {
            throw new IllegalArgumentException("无效的规则ID: " + invalidRuleIds);
        }

        // 更新配置
        systemConfigService.updateDefaultGroupRules(config);
        log.info("更新默认规则配置: ruleCount={}", config.getRuleCount());
    }

    @Override
    public List<MessageRule> getDefaultRules() {
        DefaultRuleConfigDTO config = getDefaultRuleConfig();
        if (config.ruleIds() == null || config.ruleIds().isEmpty()) {
            return Collections.emptyList();
        }

        // 查询规则实体
        List<MessageRule> rules = messageRuleMapper.selectBatchIds(config.ruleIds());
        log.debug("获取默认规则列表: count={}", rules.size());
        return rules;
    }

    @Override
    @Transactional
    public Integer applyDefaultRulesToGroup(GroupChat groupChat) {
        List<Long> defaultRuleIds = getDefaultRuleConfig().ruleIds();
        if (defaultRuleIds == null || defaultRuleIds.isEmpty()) {
            log.debug("默认规则配置为空，跳过绑定: groupId={}", groupChat.getGroupId());
            return 0;
        }

        // 查询已绑定的规则
        LambdaQueryWrapper<GroupRuleConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GroupRuleConfig::getGroupId, groupChat.getId());
        List<GroupRuleConfig> existingConfigs = groupRuleConfigMapper.selectList(queryWrapper);
        List<Long> existingRuleIds = existingConfigs.stream()
                .map(GroupRuleConfig::getRuleId)
                .collect(Collectors.toList());

        // 绑定新规则
        int bindCount = 0;
        for (Long ruleId : defaultRuleIds) {
            if (!existingRuleIds.contains(ruleId)) {
                GroupRuleConfig config = new GroupRuleConfig();
                config.setGroupId(groupChat.getId());
                config.setRuleId(ruleId);
                config.setEnabled(true);
                config.setExecutionCount(0L);
                groupRuleConfigMapper.insert(config);
                bindCount++;
            }
        }

        log.info("为群组绑定默认规则: groupId={}, groupName={}, bindCount={}",
                groupChat.getGroupId(), groupChat.getGroupName(), bindCount);
        return bindCount;
    }

    @Override
    @Transactional
    public Integer batchApplyDefaultRules(List<GroupChat> groupChats) {
        if (groupChats == null || groupChats.isEmpty()) {
            return 0;
        }

        int totalBindCount = 0;
        for (GroupChat groupChat : groupChats) {
            totalBindCount += applyDefaultRulesToGroup(groupChat);
        }

        log.info("批量绑定默认规则: groupCount={}, totalBindCount={}", groupChats.size(), totalBindCount);
        return totalBindCount;
    }

    @Override
    public List<Long> validateRuleIds(List<Long> ruleIds) {
        if (ruleIds == null || ruleIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 查询存在的规则
        List<MessageRule> existingRules = messageRuleMapper.selectBatchIds(ruleIds);
        List<Long> existingRuleIds = existingRules.stream()
                .map(MessageRule::getId)
                .collect(Collectors.toList());

        // 找出不存在的规则ID
        List<Long> invalidRuleIds = ruleIds.stream()
                .filter(ruleId -> !existingRuleIds.contains(ruleId))
                .collect(Collectors.toList());

        if (!invalidRuleIds.isEmpty()) {
            log.warn("发现无效的规则ID: {}", invalidRuleIds);
        }

        return invalidRuleIds;
    }

    @Override
    public Boolean hasDefaultRules(Long groupId) {
        // 查询群组
        GroupChat groupChat = groupChatMapper.selectById(groupId);
        if (groupChat == null) {
            log.warn("群组不存在: groupId={}", groupId);
            return false;
        }

        // 获取默认规则ID列表
        List<Long> defaultRuleIds = getDefaultRuleConfig().ruleIds();
        if (defaultRuleIds == null || defaultRuleIds.isEmpty()) {
            // 没有配置默认规则，认为已绑定
            return true;
        }

        // 查询已绑定的规则
        LambdaQueryWrapper<GroupRuleConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GroupRuleConfig::getGroupId, groupId)
                .in(GroupRuleConfig::getRuleId, defaultRuleIds);
        long boundCount = groupRuleConfigMapper.selectCount(queryWrapper);

        // 判断是否全部绑定
        boolean hasAll = boundCount == defaultRuleIds.size();
        log.debug("检查群组是否已绑定默认规则: groupId={}, hasAll={}, bound={}/{}",
                groupId, hasAll, boundCount, defaultRuleIds.size());
        return hasAll;
    }

    @Override
    public List<GroupChat> getGroupsWithoutDefaultRules() {
        // 获取默认规则ID列表
        List<Long> defaultRuleIds = getDefaultRuleConfig().ruleIds();
        if (defaultRuleIds == null || defaultRuleIds.isEmpty()) {
            log.debug("默认规则配置为空，返回空列表");
            return Collections.emptyList();
        }

        // 查询所有群组
        List<GroupChat> allGroups = groupChatMapper.selectList(null);

        // 筛选未绑定默认规则的群组
        List<GroupChat> groupsWithoutDefaultRules = new ArrayList<>();
        for (GroupChat group : allGroups) {
            if (!hasDefaultRules(group.getId())) {
                groupsWithoutDefaultRules.add(group);
            }
        }

        log.info("查询未绑定默认规则的群组: count={}", groupsWithoutDefaultRules.size());
        return groupsWithoutDefaultRules;
    }
}
