package com.specqq.chatbot.interceptor;

import com.specqq.chatbot.dto.MessageReceiveDTO;
import com.specqq.chatbot.entity.RulePolicy;

/**
 * 策略拦截器接口
 *
 * <p>用于在 handler 执行前进行策略检查（scope、rate limit、time window、role、cooldown）</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
public interface PolicyInterceptor {

    /**
     * 拦截检查
     *
     * @param message 接收到的消息
     * @param policy  规则策略配置
     * @return true=通过检查，false=拦截
     */
    boolean intercept(MessageReceiveDTO message, RulePolicy policy);

    /**
     * 获取拦截原因（当 intercept 返回 false 时调用）
     *
     * @return 拦截原因描述
     */
    String getInterceptReason();

    /**
     * 获取拦截器名称
     *
     * @return 拦截器名称
     */
    String getName();
}
