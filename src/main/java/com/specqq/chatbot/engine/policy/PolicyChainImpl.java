package com.specqq.chatbot.engine.policy;

import com.specqq.chatbot.dto.MessageReceiveDTO;
import com.specqq.chatbot.entity.RulePolicy;
import com.specqq.chatbot.interceptor.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 策略链实现类
 *
 * <p>按顺序执行策略拦截器：scope → rate limit → time window → role → cooldown</p>
 * <p>任一拦截器返回 false 则短路，不再执行后续拦截器</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Slf4j
@Component
public class PolicyChainImpl implements PolicyChain {

    private final List<PolicyInterceptor> interceptors;

    public PolicyChainImpl(
            ScopeInterceptor scopeInterceptor,
            RateLimitInterceptor rateLimitInterceptor,
            TimeWindowInterceptor timeWindowInterceptor,
            RoleInterceptor roleInterceptor,
            CooldownInterceptor cooldownInterceptor
    ) {
        // 按固定顺序添加拦截器
        this.interceptors = new ArrayList<>();
        interceptors.add(scopeInterceptor);
        interceptors.add(rateLimitInterceptor);
        interceptors.add(timeWindowInterceptor);
        interceptors.add(roleInterceptor);
        interceptors.add(cooldownInterceptor);
    }

    @Override
    public PolicyCheckResult check(MessageReceiveDTO message, RulePolicy policy) {
        if (policy == null) {
            log.debug("策略为空，跳过策略检查");
            return PolicyCheckResult.pass();
        }

        // 依次执行拦截器
        for (PolicyInterceptor interceptor : interceptors) {
            boolean passed = interceptor.intercept(message, policy);
            if (!passed) {
                String reason = interceptor.getInterceptReason();
                log.info("策略拦截: policy={}, reason={}", interceptor.getName(), reason);
                return PolicyCheckResult.fail(interceptor.getName(), reason);
            }
        }

        log.debug("策略检查通过");
        return PolicyCheckResult.pass();
    }
}
