package com.specqq.chatbot.interceptor;

import com.specqq.chatbot.dto.MessageReceiveDTO;
import com.specqq.chatbot.entity.RulePolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Cooldown 拦截器
 *
 * <p>检查规则是否在冷却期内（使用 Redis 存储冷却状态）</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CooldownInterceptor implements PolicyInterceptor {

    private final StringRedisTemplate redisTemplate;
    private String interceptReason;

    @Override
    public boolean intercept(MessageReceiveDTO message, RulePolicy policy) {
        // 如果策略为空或未启用冷却，默认通过
        if (policy == null || !Boolean.TRUE.equals(policy.getCooldownEnabled())) {
            return true;
        }

        Integer cooldownSeconds = policy.getCooldownSeconds();
        if (cooldownSeconds == null || cooldownSeconds <= 0) {
            log.warn("冷却策略已启用但冷却时间配置无效: cooldownSeconds={}", cooldownSeconds);
            return true;
        }

        // 构建冷却 Key
        String cooldownKey = buildCooldownKey(message, policy);

        // 检查是否在冷却期内
        Boolean exists = redisTemplate.hasKey(cooldownKey);
        if (Boolean.TRUE.equals(exists)) {
            Long ttl = redisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS);
            interceptReason = String.format("规则在冷却期内，剩余 %d 秒", ttl != null ? ttl : cooldownSeconds);
            log.debug("Cooldown 拦截: key={}, {}", cooldownKey, interceptReason);
            return false;
        }

        // 设置冷却标记
        redisTemplate.opsForValue().set(cooldownKey, "1", cooldownSeconds, TimeUnit.SECONDS);
        log.debug("Cooldown 检查通过，设置冷却期: key={}, ttl={}s", cooldownKey, cooldownSeconds);

        return true;
    }

    @Override
    public String getInterceptReason() {
        return interceptReason;
    }

    @Override
    public String getName() {
        return "Cooldown";
    }

    /**
     * 构建冷却 Key
     *
     * 格式: cooldown:{ruleId}:{scope}:{scopeId}
     */
    private String buildCooldownKey(MessageReceiveDTO message, RulePolicy policy) {
        String scope = policy.getScope() != null ? policy.getScope() : "USER";
        String scopeId = switch (scope.toUpperCase()) {
            case "USER" -> message.getUserId();
            case "GROUP" -> message.getGroupId();
            case "GLOBAL" -> "global";
            default -> message.getUserId();
        };

        return String.format("cooldown:%d:%s:%s", policy.getRuleId(), scope, scopeId);
    }
}
