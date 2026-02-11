package com.specqq.chatbot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.specqq.chatbot.engine.ContainsMatcher;
import com.specqq.chatbot.engine.ExactMatcher;
import com.specqq.chatbot.engine.RegexMatcher;
import com.specqq.chatbot.engine.StatisticsMatcher;
import com.specqq.chatbot.engine.RuleMatcher;
import com.specqq.chatbot.entity.GroupRuleConfig;
import com.specqq.chatbot.entity.MessageRule;
import com.specqq.chatbot.mapper.GroupRuleConfigMapper;
import com.specqq.chatbot.mapper.MessageRuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 规则服务
 *
 * @author Chatbot Router System
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleService extends ServiceImpl<MessageRuleMapper, MessageRule> {

    private final MessageRuleMapper messageRuleMapper;
    private final GroupRuleConfigMapper groupRuleConfigMapper;
    private final ExactMatcher exactMatcher;
    private final ContainsMatcher containsMatcher;
    private final RegexMatcher regexMatcher;
    private final StatisticsMatcher statisticsMatcher;
    private final PolicyService policyService;

    /**
     * 查询群聊启用的规则列表(按优先级排序)
     *
     * @param groupId 群聊ID
     * @return 规则列表
     */
    @Cacheable(value = "groupRules", key = "#groupId", cacheManager = "caffeineCacheManager")
    public List<MessageRule> getRulesByGroupId(Long groupId) {
        log.debug("Fetching rules for group: {}", groupId);
        return messageRuleMapper.selectEnabledRulesByGroupId(groupId);
    }

    /**
     * 创建规则
     *
     * @param rule 规则对象
     * @return 创建的规则
     */
    @Transactional(rollbackFor = Exception.class)
    public MessageRule createRule(MessageRule rule) {
        // 检查规则名称唯一性
        LambdaQueryWrapper<MessageRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessageRule::getName, rule.getName());
        if (messageRuleMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("规则名称已存在: " + rule.getName());
        }

        // 设置默认值
        if (rule.getPriority() == null) {
            rule.setPriority(50);
        }
        if (rule.getEnabled() == null) {
            rule.setEnabled(true);
        }

        messageRuleMapper.insert(rule);
        log.info("Created rule: id={}, name={}", rule.getId(), rule.getName());
        return rule;
    }

    /**
     * 更新规则
     *
     * @param rule 规则对象
     * @return 更新的规则
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "groupRules", allEntries = true, cacheManager = "caffeineCacheManager")
    public MessageRule updateRule(MessageRule rule) {
        MessageRule existing = messageRuleMapper.selectById(rule.getId());
        if (existing == null) {
            throw new IllegalArgumentException("规则不存在: " + rule.getId());
        }

        // 检查规则名称唯一性(排除自己)
        if (!existing.getName().equals(rule.getName())) {
            LambdaQueryWrapper<MessageRule> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MessageRule::getName, rule.getName());
            wrapper.ne(MessageRule::getId, rule.getId());
            if (messageRuleMapper.selectCount(wrapper) > 0) {
                throw new IllegalArgumentException("规则名称已存在: " + rule.getName());
            }
        }

        messageRuleMapper.updateById(rule);
        log.info("Updated rule: id={}, name={}", rule.getId(), rule.getName());
        return rule;
    }

    /**
     * 删除规则(检查使用情况)
     *
     * @param ruleId 规则ID
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "groupRules", allEntries = true, cacheManager = "caffeineCacheManager")
    public void deleteRule(Long ruleId) {
        // 检查规则是否被群聊使用
        LambdaQueryWrapper<GroupRuleConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupRuleConfig::getRuleId, ruleId);
        Long usageCount = groupRuleConfigMapper.selectCount(wrapper);

        if (usageCount > 0) {
            throw new IllegalStateException("规则正在被 " + usageCount + " 个群聊使用，无法删除");
        }

        messageRuleMapper.deleteById(ruleId);
        log.info("Deleted rule: id={}", ruleId);
    }

    /**
     * 切换规则启用状态
     *
     * @param ruleId  规则ID
     * @param enabled 是否启用
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "groupRules", allEntries = true, cacheManager = "caffeineCacheManager")
    public void toggleRuleStatus(Long ruleId, Boolean enabled) {
        MessageRule rule = messageRuleMapper.selectById(ruleId);
        if (rule == null) {
            throw new IllegalArgumentException("规则不存在: " + ruleId);
        }

        rule.setEnabled(enabled);
        messageRuleMapper.updateById(rule);
        log.info("Toggled rule status: id={}, enabled={}", ruleId, enabled);
    }

    /**
     * 分页查询规则
     *
     * @param page    分页参数
     * @param keyword 关键词(可选)
     * @param enabled 启用状态(可选)
     * @return 分页结果
     */
    public IPage<MessageRule> pageRules(Page<MessageRule> page, String keyword, Boolean enabled) {
        LambdaQueryWrapper<MessageRule> wrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(MessageRule::getName, keyword)
                .or()
                .like(MessageRule::getDescription, keyword);
        }

        if (enabled != null) {
            wrapper.eq(MessageRule::getEnabled, enabled);
        }

        wrapper.orderByDesc(MessageRule::getPriority)
            .orderByDesc(MessageRule::getCreateTime);

        return messageRuleMapper.selectPage(page, wrapper);
    }

    /**
     * 分页查询规则（支持多条件筛选）
     *
     * @param page      页码
     * @param size      每页数量
     * @param name      规则名称（模糊查询）
     * @param matchType 匹配类型
     * @param enabled   启用状态
     * @return 分页结果
     */
    public Page<MessageRule> listRules(Integer page, Integer size, String name, String matchType, Boolean enabled) {
        Page<MessageRule> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<MessageRule> wrapper = new LambdaQueryWrapper<>();

        if (name != null && !name.trim().isEmpty()) {
            wrapper.like(MessageRule::getName, name);
        }

        if (matchType != null && !matchType.trim().isEmpty()) {
            try {
                MessageRule.MatchType type = MessageRule.MatchType.valueOf(matchType.toUpperCase());
                wrapper.eq(MessageRule::getMatchType, type);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid match type: {}", matchType);
            }
        }

        if (enabled != null) {
            wrapper.eq(MessageRule::getEnabled, enabled);
        }

        wrapper.orderByDesc(MessageRule::getPriority)
            .orderByDesc(MessageRule::getCreateTime);

        return messageRuleMapper.selectPage(pageParam, wrapper);
    }

    /**
     * 根据ID查询规则
     *
     * @param id 规则ID
     * @return 规则对象，不存在返回null
     */
    public MessageRule getRuleById(Long id) {
        return messageRuleMapper.selectById(id);
    }

    /**
     * 检查规则名称是否已存在
     *
     * @param name 规则名称
     * @return true表示已存在
     */
    public boolean existsByName(String name) {
        LambdaQueryWrapper<MessageRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessageRule::getName, name);
        return messageRuleMapper.selectCount(wrapper) > 0;
    }

    /**
     * 验证正则表达式是否有效
     *
     * @param pattern 正则表达式
     * @return true表示有效
     */
    public boolean validateRegexPattern(String pattern) {
        try {
            Pattern.compile(pattern);
            return true;
        } catch (PatternSyntaxException e) {
            log.warn("Invalid regex pattern: {}, error: {}", pattern, e.getMessage());
            return false;
        }
    }

    /**
     * 检查规则是否正在被使用
     *
     * @param ruleId 规则ID
     * @return true表示正在使用
     */
    public boolean isRuleInUse(Long ruleId) {
        LambdaQueryWrapper<GroupRuleConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupRuleConfig::getRuleId, ruleId);
        return groupRuleConfigMapper.selectCount(wrapper) > 0;
    }

    /**
     * 批量删除规则
     *
     * @param ids 规则ID列表
     * @return 删除的数量
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "groupRules", allEntries = true, cacheManager = "caffeineCacheManager")
    public int batchDeleteRules(List<Long> ids) {
        int count = 0;
        for (Long id : ids) {
            messageRuleMapper.deleteById(id);
            count++;
        }
        log.info("Batch deleted {} rules", count);
        return count;
    }

    /**
     * 复制规则
     *
     * @param sourceId 源规则ID
     * @param newName  新规则名称
     * @return 复制的规则
     */
    @Transactional(rollbackFor = Exception.class)
    public MessageRule copyRule(Long sourceId, String newName) {
        MessageRule source = messageRuleMapper.selectById(sourceId);
        if (source == null) {
            throw new IllegalArgumentException("源规则不存在: " + sourceId);
        }

        MessageRule copy = new MessageRule();
        copy.setName(newName);
        copy.setDescription(source.getDescription() + " (副本)");
        copy.setMatchType(source.getMatchType());
        copy.setPattern(source.getPattern());
        copy.setResponseTemplate(source.getResponseTemplate());
        copy.setPriority(source.getPriority());
        copy.setEnabled(false); // 默认禁用
        copy.setCreatedBy(source.getCreatedBy());

        messageRuleMapper.insert(copy);
        log.info("Copied rule: sourceId={}, newId={}, newName={}", sourceId, copy.getId(), newName);
        return copy;
    }

    /**
     * 测试规则匹配
     *
     * @param matchType 匹配类型
     * @param pattern   匹配模式
     * @param message   测试消息
     * @return true表示匹配成功
     */
    public boolean testRuleMatch(MessageRule.MatchType matchType, String pattern, String message) {
        RuleMatcher matcher = switch (matchType) {
            case EXACT -> exactMatcher;
            case CONTAINS -> containsMatcher;
            case REGEX -> regexMatcher;
            case STATISTICS -> statisticsMatcher;
            case PREFIX, SUFFIX -> regexMatcher; // PREFIX and SUFFIX use regex matcher
        };

        return matcher.matches(message, pattern);
    }

    /**
     * 创建规则（带策略）
     *
     * @param rule   规则对象
     * @param policy 策略对象（可选）
     * @return 创建的规则
     */
    @Transactional(rollbackFor = Exception.class)
    public MessageRule createRuleWithPolicy(MessageRule rule, com.specqq.chatbot.entity.RulePolicy policy) {
        // 创建规则
        MessageRule created = createRule(rule);

        // 如果提供了策略，创建策略
        if (policy != null) {
            policy.setRuleId(created.getId());
            String validationError = policyService.validatePolicy(policy);
            if (validationError != null) {
                throw new IllegalArgumentException("策略配置无效: " + validationError);
            }
            policyService.createPolicy(policy);
        }

        return created;
    }

    /**
     * 更新规则（带策略）
     *
     * @param rule   规则对象
     * @param policy 策略对象（可选）
     * @return 更新的规则
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "groupRules", allEntries = true, cacheManager = "caffeineCacheManager")
    public MessageRule updateRuleWithPolicy(MessageRule rule, com.specqq.chatbot.entity.RulePolicy policy) {
        // 更新规则
        MessageRule updated = updateRule(rule);

        // 如果提供了策略，创建或更新策略
        if (policy != null) {
            policy.setRuleId(updated.getId());
            String validationError = policyService.validatePolicy(policy);
            if (validationError != null) {
                throw new IllegalArgumentException("策略配置无效: " + validationError);
            }
            policyService.saveOrUpdatePolicy(policy);
        }

        return updated;
    }

    /**
     * 获取规则详情（包含策略）
     *
     * @param ruleId 规则 ID
     * @return 规则对象和策略
     */
    public RuleWithPolicy getRuleWithPolicy(Long ruleId) {
        MessageRule rule = messageRuleMapper.selectById(ruleId);
        if (rule == null) {
            return null;
        }

        com.specqq.chatbot.entity.RulePolicy policy = policyService.getPolicyByRuleId(ruleId);
        return new RuleWithPolicy(rule, policy);
    }

    /**
     * 规则和策略的组合对象
     */
    public record RuleWithPolicy(MessageRule rule, com.specqq.chatbot.entity.RulePolicy policy) {
    }
}
