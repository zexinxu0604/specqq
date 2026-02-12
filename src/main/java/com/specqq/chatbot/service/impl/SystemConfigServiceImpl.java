package com.specqq.chatbot.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.specqq.chatbot.constant.SystemConfigKeys;
import com.specqq.chatbot.dto.DefaultRuleConfigDTO;
import com.specqq.chatbot.entity.SystemConfig;
import com.specqq.chatbot.mapper.SystemConfigMapper;
import com.specqq.chatbot.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collections;

/**
 * 系统配置服务实现
 *
 * @author Claude Code
 * @since 2026-02-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigMapper systemConfigMapper;
    private final ObjectMapper objectMapper;

    /**
     * Caffeine 缓存实例
     * 缓存系统配置，减少数据库查询
     */
    private final Cache<String, Object> configCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(100)
            .build();

    @Override
    public DefaultRuleConfigDTO getDefaultGroupRules() {
        return getConfig(SystemConfigKeys.DEFAULT_GROUP_RULES, DefaultRuleConfigDTO.class);
    }

    @Override
    @Transactional
    public void updateDefaultGroupRules(DefaultRuleConfigDTO config) {
        updateConfig(SystemConfigKeys.DEFAULT_GROUP_RULES, config);
    }

    @Override
    public <T> T getSyncTaskConfig(Class<T> tClass) {
        return getConfig(SystemConfigKeys.SYNC_TASK_CONFIG, tClass);
    }

    @Override
    @Transactional
    public void updateSyncTaskConfig(Object config) {
        updateConfig(SystemConfigKeys.SYNC_TASK_CONFIG, config);
    }

    @Override
    public <T> T getRetryPolicyConfig(Class<T> tClass) {
        return getConfig(SystemConfigKeys.RETRY_POLICY_CONFIG, tClass);
    }

    @Override
    @Transactional
    public void updateRetryPolicyConfig(Object config) {
        updateConfig(SystemConfigKeys.RETRY_POLICY_CONFIG, config);
    }

    @Override
    public <T> T getConfig(String configKey, Class<T> tClass) {
        // 先从缓存获取
        Object cached = configCache.getIfPresent(configKey);
        if (cached != null && tClass.isInstance(cached)) {
            log.debug("从缓存获取配置: key={}", configKey);
            return tClass.cast(cached);
        }

        // 从数据库查询
        SystemConfig systemConfig = systemConfigMapper.selectByConfigKey(configKey);
        if (systemConfig == null) {
            log.warn("配置不存在: key={}", configKey);
            return getDefaultConfig(configKey, tClass);
        }

        try {
            // 将 JSON 配置值转换为目标类型
            T configValue = objectMapper.convertValue(systemConfig.getConfigValue(), tClass);
            // 放入缓存
            configCache.put(configKey, configValue);
            log.debug("从数据库获取配置并缓存: key={}", configKey);
            return configValue;
        } catch (Exception e) {
            log.error("配置值转换失败: key={}, value={}", configKey, systemConfig.getConfigValue(), e);
            return getDefaultConfig(configKey, tClass);
        }
    }

    @Override
    @Transactional
    public void updateConfig(String configKey, Object configValue) {
        try {
            String jsonValue = objectMapper.writeValueAsString(configValue);

            SystemConfig existing = systemConfigMapper.selectByConfigKey(configKey);
            if (existing != null) {
                // 更新现有配置
                int updated = systemConfigMapper.updateConfigValue(configKey, jsonValue);
                if (updated > 0) {
                    log.info("更新配置成功: key={}", configKey);
                } else {
                    log.warn("更新配置失败: key={}", configKey);
                }
            } else {
                // 插入新配置
                SystemConfig newConfig = new SystemConfig();
                newConfig.setConfigKey(configKey);
                newConfig.setConfigValue(configValue);
                newConfig.setConfigType(configValue.getClass().getSimpleName());
                newConfig.setDescription("Auto-generated config for " + configKey);
                systemConfigMapper.insert(newConfig);
                log.info("创建新配置: key={}", configKey);
            }

            // 更新缓存
            configCache.put(configKey, configValue);
        } catch (JsonProcessingException e) {
            log.error("配置值序列化失败: key={}, value={}", configKey, configValue, e);
            throw new RuntimeException("Failed to serialize config value", e);
        }
    }

    @Override
    public void clearCache() {
        configCache.invalidateAll();
        log.info("清除所有配置缓存");
    }

    /**
     * 获取默认配置
     * 当数据库中不存在配置时返回默认值
     *
     * @param configKey 配置键
     * @param tClass    配置类型
     * @return 默认配置值
     */
    private <T> T getDefaultConfig(String configKey, Class<T> tClass) {
        if (SystemConfigKeys.DEFAULT_GROUP_RULES.equals(configKey)) {
            // 默认规则配置为空列表
            return tClass.cast(DefaultRuleConfigDTO.of(Collections.emptyList()));
        }
        // 其他配置返回 null
        return null;
    }
}
