package com.specqq.chatbot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.specqq.chatbot.entity.GroupChat;
import com.specqq.chatbot.entity.GroupRuleConfig;
import com.specqq.chatbot.entity.MessageLog;
import com.specqq.chatbot.mapper.GroupChatMapper;
import com.specqq.chatbot.mapper.GroupRuleConfigMapper;
import com.specqq.chatbot.mapper.MessageLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 群聊服务
 *
 * @author Chatbot Router System
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService extends ServiceImpl<GroupChatMapper, GroupChat> {

    private final GroupChatMapper groupChatMapper;
    private final GroupRuleConfigMapper groupRuleConfigMapper;
    private final MessageLogMapper messageLogMapper;

    /**
     * 根据群聊平台ID查询群聊
     *
     * @param groupId 群聊平台ID(如QQ群号)
     * @return 群聊对象
     */
    @Cacheable(value = "groups", key = "#groupId", cacheManager = "caffeineCacheManager")
    public GroupChat getGroupByGroupId(String groupId) {
        LambdaQueryWrapper<GroupChat> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupChat::getGroupId, groupId);
        return groupChatMapper.selectOne(wrapper);
    }

    /**
     * 分页查询群聊列表
     *
     * @param page     分页参数
     * @param clientId 客户端ID(可选)
     * @param enabled  启用状态(可选)
     * @param keyword  关键词(可选)
     * @return 分页结果
     */
    public IPage<GroupChat> listGroups(Page<GroupChat> page, Long clientId, Boolean enabled, String keyword) {
        LambdaQueryWrapper<GroupChat> wrapper = new LambdaQueryWrapper<>();

        if (clientId != null) {
            wrapper.eq(GroupChat::getClientId, clientId);
        }

        if (enabled != null) {
            wrapper.eq(GroupChat::getEnabled, enabled);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(GroupChat::getGroupName, keyword)
                .or()
                .like(GroupChat::getGroupId, keyword);
        }

        wrapper.orderByDesc(GroupChat::getUpdatedAt);

        return groupChatMapper.selectPage(page, wrapper);
    }

    /**
     * 更新群聊配置
     *
     * @param groupId 群聊ID
     * @param config  群聊配置
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "groups", key = "#groupId", cacheManager = "caffeineCacheManager")
    public void updateGroupConfig(Long groupId, GroupChat.GroupConfig config) {
        GroupChat group = groupChatMapper.selectById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("群聊不存在: " + groupId);
        }

        group.setConfig(config);
        groupChatMapper.updateById(group);
        log.info("Updated group config: id={}, groupId={}", groupId, group.getGroupId());
    }

    /**
     * 切换群聊启用状态
     *
     * @param groupId 群聊ID
     * @param enabled 是否启用
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"groups", "groupRules"}, allEntries = true, cacheManager = "caffeineCacheManager")
    public void toggleGroupStatus(Long groupId, Boolean enabled) {
        GroupChat group = groupChatMapper.selectById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("群聊不存在: " + groupId);
        }

        group.setEnabled(enabled);
        groupChatMapper.updateById(group);
        log.info("Toggled group status: id={}, groupId={}, enabled={}", groupId, group.getGroupId(), enabled);
    }

    /**
     * 获取群聊的规则配置列表
     *
     * @param groupId 群聊ID
     * @return 规则配置列表
     */
    public List<GroupRuleConfig> getGroupRuleConfigs(Long groupId) {
        LambdaQueryWrapper<GroupRuleConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupRuleConfig::getGroupId, groupId);
        wrapper.orderByDesc(GroupRuleConfig::getCreatedAt);
        return groupRuleConfigMapper.selectList(wrapper);
    }

    /**
     * 为群聊启用规则
     *
     * @param groupId 群聊ID
     * @param ruleId  规则ID
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "groupRules", key = "#groupId", cacheManager = "caffeineCacheManager")
    public void enableRuleForGroup(Long groupId, Long ruleId) {
        // 检查是否已存在
        LambdaQueryWrapper<GroupRuleConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupRuleConfig::getGroupId, groupId);
        wrapper.eq(GroupRuleConfig::getRuleId, ruleId);
        GroupRuleConfig existing = groupRuleConfigMapper.selectOne(wrapper);

        if (existing != null) {
            // 已存在则更新启用状态
            existing.setEnabled(true);
            groupRuleConfigMapper.updateById(existing);
        } else {
            // 不存在则创建
            GroupRuleConfig config = new GroupRuleConfig();
            config.setGroupId(groupId);
            config.setRuleId(ruleId);
            config.setEnabled(true);
            config.setExecutionCount(0L);
            groupRuleConfigMapper.insert(config);
        }

        log.info("Enabled rule for group: groupId={}, ruleId={}", groupId, ruleId);
    }

    /**
     * 为群聊禁用规则
     *
     * @param groupId 群聊ID
     * @param ruleId  规则ID
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "groupRules", key = "#groupId", cacheManager = "caffeineCacheManager")
    public void disableRuleForGroup(Long groupId, Long ruleId) {
        LambdaQueryWrapper<GroupRuleConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupRuleConfig::getGroupId, groupId);
        wrapper.eq(GroupRuleConfig::getRuleId, ruleId);
        GroupRuleConfig config = groupRuleConfigMapper.selectOne(wrapper);

        if (config != null) {
            config.setEnabled(false);
            groupRuleConfigMapper.updateById(config);
            log.info("Disabled rule for group: groupId={}, ruleId={}", groupId, ruleId);
        }
    }

    /**
     * 分页查询群聊列表（支持多条件筛选）
     *
     * @param page      页码
     * @param size      每页数量
     * @param keyword   关键词（群名称或群ID）
     * @param clientId  客户端ID
     * @param enabled   启用状态
     * @return 分页结果
     */
    public Page<GroupChat> listGroups(Integer page, Integer size, String keyword, Long clientId, Boolean enabled) {
        Page<GroupChat> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<GroupChat> wrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(GroupChat::getGroupName, keyword)
                .or()
                .like(GroupChat::getGroupId, keyword));
        }

        if (clientId != null) {
            wrapper.eq(GroupChat::getClientId, clientId);
        }

        if (enabled != null) {
            wrapper.eq(GroupChat::getEnabled, enabled);
        }

        wrapper.orderByDesc(GroupChat::getUpdatedAt);

        return groupChatMapper.selectPage(pageParam, wrapper);
    }

    /**
     * 根据ID查询群聊
     *
     * @param id 群聊ID
     * @return 群聊对象，不存在返回null
     */
    public GroupChat getGroupById(Long id) {
        return groupChatMapper.selectById(id);
    }
    /**
     * 批量启用规则
     *
     * @param groupId 群聊ID
     * @param ruleIds 规则ID列表
     * @param enabled 启用状态
     * @return 操作的数量
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "groupRules", allEntries = true, cacheManager = "caffeineCacheManager")
    public int batchEnableRules(Long groupId, List<Long> ruleIds, Boolean enabled) {
        int count = 0;
        for (Long ruleId : ruleIds) {
            if (enabled) {
                enableRuleForGroup(groupId, ruleId);
            } else {
                disableRuleForGroup(groupId, ruleId);
            }
            count++;
        }
        log.info("Batch {} rules for group: groupId={}, count={}", enabled ? "enabled" : "disabled", groupId, count);
        return count;
    }

    /**
     * 为群聊添加规则
     *
     * @param groupId 群聊ID
     * @param ruleId  规则ID
     * @return 规则配置对象
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "groupRules", allEntries = true, cacheManager = "caffeineCacheManager")
    public GroupRuleConfig addRuleToGroup(Long groupId, Long ruleId) {
        // 检查是否已存在
        LambdaQueryWrapper<GroupRuleConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupRuleConfig::getGroupId, groupId);
        wrapper.eq(GroupRuleConfig::getRuleId, ruleId);
        GroupRuleConfig existing = groupRuleConfigMapper.selectOne(wrapper);

        if (existing != null) {
            existing.setEnabled(true);
            groupRuleConfigMapper.updateById(existing);
            return existing;
        }

        GroupRuleConfig config = new GroupRuleConfig();
        config.setGroupId(groupId);
        config.setRuleId(ruleId);
        config.setEnabled(true);
        config.setExecutionCount(0L);
        groupRuleConfigMapper.insert(config);

        log.info("Added rule to group: groupId={}, ruleId={}", groupId, ruleId);
        return config;
    }

    /**
     * 从群聊移除规则
     *
     * @param groupId 群聊ID
     * @param ruleId  规则ID
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "groupRules", allEntries = true, cacheManager = "caffeineCacheManager")
    public void removeRuleFromGroup(Long groupId, Long ruleId) {
        LambdaQueryWrapper<GroupRuleConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupRuleConfig::getGroupId, groupId);
        wrapper.eq(GroupRuleConfig::getRuleId, ruleId);

        groupRuleConfigMapper.delete(wrapper);
        log.info("Removed rule from group: groupId={}, ruleId={}", groupId, ruleId);
    }

    /**
     * 切换群聊规则启用状态
     *
     * @param groupId 群聊ID
     * @param ruleId  规则ID
     * @param enabled 启用状态
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "groupRules", allEntries = true, cacheManager = "caffeineCacheManager")
    public void toggleGroupRuleStatus(Long groupId, Long ruleId, Boolean enabled) {
        LambdaQueryWrapper<GroupRuleConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupRuleConfig::getGroupId, groupId);
        wrapper.eq(GroupRuleConfig::getRuleId, ruleId);
        GroupRuleConfig config = groupRuleConfigMapper.selectOne(wrapper);

        if (config != null) {
            config.setEnabled(enabled);
            groupRuleConfigMapper.updateById(config);
            log.info("Toggled group rule status: groupId={}, ruleId={}, enabled={}", groupId, ruleId, enabled);
        }
    }

    /**
     * 查询群聊统计信息
     *
     * @param groupId   群聊ID
     * @param startTime 开始时间（ISO 8601格式）
     * @param endTime   结束时间（ISO 8601格式）
     * @return 统计信息
     */
    public Map<String, Object> getGroupStats(Long groupId, String startTime, String endTime) {
        Map<String, Object> stats = new HashMap<>();

        // 解析时间范围
        LocalDateTime start = startTime != null ?
            LocalDateTime.parse(startTime, DateTimeFormatter.ISO_DATE_TIME) :
            LocalDateTime.now().minusDays(7);

        LocalDateTime end = endTime != null ?
            LocalDateTime.parse(endTime, DateTimeFormatter.ISO_DATE_TIME) :
            LocalDateTime.now();

        // 查询总消息数
        LambdaQueryWrapper<MessageLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessageLog::getGroupId, groupId);
        wrapper.between(MessageLog::getTimestamp, start, end);
        Long totalMessages = messageLogMapper.selectCount(wrapper);

        // 查询成功回复数
        LambdaQueryWrapper<MessageLog> successWrapper = new LambdaQueryWrapper<>();
        successWrapper.eq(MessageLog::getGroupId, groupId);
        successWrapper.eq(MessageLog::getSendStatus, MessageLog.SendStatus.SUCCESS);
        successWrapper.between(MessageLog::getTimestamp, start, end);
        Long successReplies = messageLogMapper.selectCount(successWrapper);

        // 查询失败回复数
        LambdaQueryWrapper<MessageLog> failedWrapper = new LambdaQueryWrapper<>();
        failedWrapper.eq(MessageLog::getGroupId, groupId);
        failedWrapper.eq(MessageLog::getSendStatus, MessageLog.SendStatus.FAILED);
        failedWrapper.between(MessageLog::getTimestamp, start, end);
        Long failedReplies = messageLogMapper.selectCount(failedWrapper);

        // 查询跳过回复数（频率限制）
        LambdaQueryWrapper<MessageLog> skippedWrapper = new LambdaQueryWrapper<>();
        skippedWrapper.eq(MessageLog::getGroupId, groupId);
        skippedWrapper.eq(MessageLog::getSendStatus, MessageLog.SendStatus.SKIPPED);
        skippedWrapper.between(MessageLog::getTimestamp, start, end);
        Long skippedReplies = messageLogMapper.selectCount(skippedWrapper);

        stats.put("totalMessages", totalMessages);
        stats.put("successReplies", successReplies);
        stats.put("failedReplies", failedReplies);
        stats.put("skippedReplies", skippedReplies);
        stats.put("startTime", start);
        stats.put("endTime", end);

        return stats;
    }

    /**
     * 同步群聊信息（从客户端）
     *
     * @param groupId 群聊ID
     * @return 同步后的群聊对象
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "groups", allEntries = true, cacheManager = "caffeineCacheManager")
    public GroupChat syncGroupInfo(Long groupId) {
        GroupChat group = groupChatMapper.selectById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("群聊不存在: " + groupId);
        }

        // TODO: 实际实现需要调用客户端API获取最新信息
        // 这里仅作为占位符
        log.info("Synced group info: id={}, groupId={}", groupId, group.getGroupId());
        return group;
    }

    /**
     * 批量导入群聊（从客户端）
     *
     * @param clientId 客户端ID
     * @return 导入结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchImportGroups(Long clientId) {
        Map<String, Object> result = new HashMap<>();

        // TODO: 实际实现需要调用客户端API获取群聊列表
        // 这里仅作为占位符
        result.put("clientId", clientId);
        result.put("imported", 0);
        result.put("skipped", 0);
        result.put("message", "批量导入功能待实现");

        log.info("Batch import groups: clientId={}", clientId);
        return result;
    }
}
