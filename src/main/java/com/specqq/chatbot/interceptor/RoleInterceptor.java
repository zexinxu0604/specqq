package com.specqq.chatbot.interceptor;

import com.specqq.chatbot.dto.MessageReceiveDTO;
import com.specqq.chatbot.entity.RulePolicy;
import com.specqq.chatbot.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Role 拦截器
 *
 * <p>检查用户是否具有规则要求的角色</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoleInterceptor implements PolicyInterceptor {

    private final UserRoleService userRoleService;
    private String interceptReason;

    @Override
    public boolean intercept(MessageReceiveDTO message, RulePolicy policy) {
        // 如果策略为空或未启用角色限制，默认通过
        if (policy == null || !Boolean.TRUE.equals(policy.getRoleEnabled())) {
            return true;
        }

        // 检查是否配置了允许的角色列表
        if (policy.getAllowedRoles() == null || policy.getAllowedRoles().isEmpty()) {
            log.warn("角色策略已启用但未配置允许的角色列表");
            return true;
        }

        // 获取用户角色
        String userRole = userRoleService.getUserRole(message.getGroupId(), message.getUserId());

        if (userRole == null) {
            interceptReason = "无法获取用户角色信息";
            log.debug("Role 拦截: {}", interceptReason);
            return false;
        }

        // 检查用户角色是否在允许列表中
        if (!policy.getAllowedRoles().contains(userRole)) {
            interceptReason = String.format("用户角色 %s 不在允许的角色列表中: %s",
                    userRole, policy.getAllowedRoles());
            log.debug("Role 拦截: {}", interceptReason);
            return false;
        }

        return true;
    }

    @Override
    public String getInterceptReason() {
        return interceptReason;
    }

    @Override
    public String getName() {
        return "Role";
    }
}
