package com.specqq.chatbot.service;

import com.specqq.chatbot.dto.DefaultRuleConfigDTO;

/**
 * 系统配置服务接口
 * 提供系统配置的查询和更新操作
 *
 * @author Claude Code
 * @since 2026-02-12
 */
public interface SystemConfigService {

    /**
     * 获取默认群组规则配置
     *
     * @return 默认规则配置，如果不存在则返回空配置
     */
    DefaultRuleConfigDTO getDefaultGroupRules();

    /**
     * 更新默认群组规则配置
     *
     * @param config 新的默认规则配置
     */
    void updateDefaultGroupRules(DefaultRuleConfigDTO config);

    /**
     * 获取同步任务配置
     *
     * @param <T>    配置类型
     * @param tClass 配置类的 Class 对象
     * @return 同步任务配置
     */
    <T> T getSyncTaskConfig(Class<T> tClass);

    /**
     * 更新同步任务配置
     *
     * @param config 新的同步任务配置
     */
    void updateSyncTaskConfig(Object config);

    /**
     * 获取重试策略配置
     *
     * @param <T>    配置类型
     * @param tClass 配置类的 Class 对象
     * @return 重试策略配置
     */
    <T> T getRetryPolicyConfig(Class<T> tClass);

    /**
     * 更新重试策略配置
     *
     * @param config 新的重试策略配置
     */
    void updateRetryPolicyConfig(Object config);

    /**
     * 根据配置键获取配置值
     *
     * @param configKey 配置键
     * @param tClass    配置类的 Class 对象
     * @param <T>       配置类型
     * @return 配置值，如果不存在则返回 null
     */
    <T> T getConfig(String configKey, Class<T> tClass);

    /**
     * 根据配置键更新配置值
     *
     * @param configKey   配置键
     * @param configValue 新配置值
     */
    void updateConfig(String configKey, Object configValue);

    /**
     * 清除配置缓存
     */
    void clearCache();
}
