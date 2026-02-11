package com.specqq.chatbot.interceptor;

import com.specqq.chatbot.dto.MessageReceiveDTO;
import com.specqq.chatbot.entity.RulePolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Scope 拦截器
 *
 * <p>检查消息是否在规则的作用域内（白名单/黑名单）</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Slf4j
@Component
public class ScopeInterceptor implements PolicyInterceptor {

    private String interceptReason;

    @Override
    public boolean intercept(MessageReceiveDTO message, RulePolicy policy) {
        // 如果策略为空，默认通过
        if (policy == null) {
            return true;
        }

        String scope = policy.getScope();
        if (scope == null) {
            scope = "USER";
        }

        // 获取消息来源标识
        String sourceId = getSourceId(message, scope);

        // 检查黑名单
        if (policy.getBlacklist() != null && !policy.getBlacklist().isEmpty()) {
            if (policy.getBlacklist().contains(sourceId)) {
                interceptReason = String.format("来源 %s 在黑名单中", sourceId);
                log.debug("Scope 拦截: {}", interceptReason);
                return false;
            }
        }

        // 检查白名单
        if (policy.getWhitelist() != null && !policy.getWhitelist().isEmpty()) {
            if (!policy.getWhitelist().contains(sourceId)) {
                interceptReason = String.format("来源 %s 不在白名单中", sourceId);
                log.debug("Scope 拦截: {}", interceptReason);
                return false;
            }
        }

        return true;
    }

    @Override
    public String getInterceptReason() {
        return interceptReason;
    }

    @Override
    public String getName() {
        return "Scope";
    }

    /**
     * 根据作用域类型获取来源标识
     */
    private String getSourceId(MessageReceiveDTO message, String scope) {
        return switch (scope.toUpperCase()) {
            case "USER" -> message.getUserId(); // 用户ID
            case "GROUP" -> message.getGroupId(); // 群ID
            case "GLOBAL" -> "*"; // 全局作用域
            default -> message.getUserId();
        };
    }
}
