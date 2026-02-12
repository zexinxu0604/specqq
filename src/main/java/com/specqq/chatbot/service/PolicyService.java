package com.specqq.chatbot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.specqq.chatbot.entity.RulePolicy;
import com.specqq.chatbot.mapper.PolicyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 策略服务
 *
 * <p>管理规则策略的 CRUD 操作和缓存</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyService extends ServiceImpl<PolicyMapper, RulePolicy> {

    private final PolicyMapper policyMapper;

    /**
     * 根据规则 ID 查询策略（带缓存）
     *
     * @param ruleId 规则 ID
     * @return 策略对象，不存在返回 null
     */
    @Cacheable(value = "ruleGroupCache", key = "'policy:' + #ruleId", cacheManager = "caffeineCacheManager")
    public RulePolicy getPolicyByRuleId(Long ruleId) {
        log.debug("查询规则策略: ruleId={}", ruleId);

        LambdaQueryWrapper<RulePolicy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RulePolicy::getRuleId, ruleId);

        return policyMapper.selectOne(wrapper);
    }

    /**
     * 创建策略
     *
     * @param policy 策略对象
     * @return 创建的策略
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "ruleGroupCache", key = "'policy:' + #policy.ruleId", cacheManager = "caffeineCacheManager")
    public RulePolicy createPolicy(RulePolicy policy) {
        // 检查规则是否已有策略
        RulePolicy existing = getPolicyByRuleId(policy.getRuleId());
        if (existing != null) {
            throw new IllegalStateException("规则已存在策略配置: ruleId=" + policy.getRuleId());
        }

        // 设置默认值
        if (policy.getScope() == null) {
            policy.setScope("USER");
        }
        if (policy.getRateLimitEnabled() == null) {
            policy.setRateLimitEnabled(false);
        }
        if (policy.getTimeWindowEnabled() == null) {
            policy.setTimeWindowEnabled(false);
        }
        if (policy.getRoleEnabled() == null) {
            policy.setRoleEnabled(false);
        }
        if (policy.getCooldownEnabled() == null) {
            policy.setCooldownEnabled(false);
        }

        policyMapper.insert(policy);
        log.info("创建策略: id={}, ruleId={}", policy.getId(), policy.getRuleId());

        return policy;
    }

    /**
     * 更新策略
     *
     * @param policy 策略对象
     * @return 更新的策略
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "ruleGroupCache", key = "'policy:' + #policy.ruleId", cacheManager = "caffeineCacheManager")
    public RulePolicy updatePolicy(RulePolicy policy) {
        RulePolicy existing = policyMapper.selectById(policy.getId());
        if (existing == null) {
            throw new IllegalArgumentException("策略不存在: id=" + policy.getId());
        }

        policyMapper.updateById(policy);
        log.info("更新策略: id={}, ruleId={}", policy.getId(), policy.getRuleId());

        return policy;
    }

    /**
     * 删除策略
     *
     * @param ruleId 规则 ID
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "ruleGroupCache", key = "'policy:' + #ruleId", cacheManager = "caffeineCacheManager")
    public void deletePolicyByRuleId(Long ruleId) {
        LambdaQueryWrapper<RulePolicy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RulePolicy::getRuleId, ruleId);

        policyMapper.delete(wrapper);
        log.info("删除策略: ruleId={}", ruleId);
    }

    /**
     * 创建或更新策略
     *
     * <p>如果策略不存在则创建，存在则更新</p>
     *
     * @param policy 策略对象
     * @return 保存的策略
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "ruleGroupCache", key = "'policy:' + #policy.ruleId", cacheManager = "caffeineCacheManager")
    public RulePolicy saveOrUpdatePolicy(RulePolicy policy) {
        RulePolicy existing = getPolicyByRuleId(policy.getRuleId());

        if (existing == null) {
            return createPolicy(policy);
        } else {
            policy.setId(existing.getId());
            return updatePolicy(policy);
        }
    }

    /**
     * 验证策略配置
     *
     * <p>检查策略配置的合法性</p>
     *
     * @param policy 策略对象
     * @return 验证结果消息，null 表示验证通过
     */
    public String validatePolicy(RulePolicy policy) {
        // Rate Limit 验证
        if (Boolean.TRUE.equals(policy.getRateLimitEnabled())) {
            if (policy.getRateLimitMaxRequests() == null || policy.getRateLimitMaxRequests() <= 0) {
                return "限流策略已启用，但最大请求数无效";
            }
            if (policy.getRateLimitWindowSeconds() == null || policy.getRateLimitWindowSeconds() <= 0) {
                return "限流策略已启用，但时间窗口无效";
            }
        }

        // Time Window 验证
        if (Boolean.TRUE.equals(policy.getTimeWindowEnabled())) {
            if (policy.getTimeWindowStart() == null || policy.getTimeWindowEnd() == null) {
                return "时间窗口策略已启用，但时间范围未设置";
            }
        }

        // Role 验证
        if (Boolean.TRUE.equals(policy.getRoleEnabled())) {
            if (policy.getAllowedRoles() == null || policy.getAllowedRoles().isEmpty()) {
                return "角色策略已启用，但允许的角色列表为空";
            }
        }

        // Cooldown 验证
        if (Boolean.TRUE.equals(policy.getCooldownEnabled())) {
            if (policy.getCooldownSeconds() == null || policy.getCooldownSeconds() <= 0) {
                return "冷却策略已启用，但冷却时间无效";
            }
        }

        return null; // 验证通过
    }
}
