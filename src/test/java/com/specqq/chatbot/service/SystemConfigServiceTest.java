package com.specqq.chatbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specqq.chatbot.constant.SystemConfigKeys;
import com.specqq.chatbot.dto.DefaultRuleConfigDTO;
import com.specqq.chatbot.entity.SystemConfig;
import com.specqq.chatbot.mapper.SystemConfigMapper;
import com.specqq.chatbot.service.impl.SystemConfigServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SystemConfigService 单元测试
 *
 * @author Claude Code
 * @since 2026-02-12
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("系统配置服务测试")
class SystemConfigServiceTest {

    @Mock
    private SystemConfigMapper systemConfigMapper;

    @Mock
    private ObjectMapper objectMapper;

    private SystemConfigService systemConfigService;

    @BeforeEach
    void setUp() {
        // 初始化服务
        systemConfigService = new SystemConfigServiceImpl(
            systemConfigMapper,
            objectMapper
        );
    }

    @Test
    @DisplayName("获取默认群组规则配置 - 配置存在")
    void testGetDefaultGroupRules_ConfigExists() {
        // Given
        SystemConfig config = new SystemConfig();
        config.setConfigKey(SystemConfigKeys.DEFAULT_GROUP_RULES);
        Map<String, Object> configValue = new HashMap<>();
        configValue.put("ruleIds", Arrays.asList(1, 2, 3));
        config.setConfigValue(configValue);

        when(systemConfigMapper.selectByConfigKey(SystemConfigKeys.DEFAULT_GROUP_RULES)).thenReturn(config);
        when(objectMapper.convertValue(any(), eq(DefaultRuleConfigDTO.class)))
                .thenReturn(DefaultRuleConfigDTO.of(Arrays.asList(1L, 2L, 3L)));

        // When
        DefaultRuleConfigDTO result = systemConfigService.getDefaultGroupRules();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.ruleIds()).hasSize(3);
        assertThat(result.ruleIds()).containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("获取默认群组规则配置 - 配置不存在")
    void testGetDefaultGroupRules_ConfigNotExists() {
        // Given
        when(systemConfigMapper.selectByConfigKey(SystemConfigKeys.DEFAULT_GROUP_RULES)).thenReturn(null);

        // When
        DefaultRuleConfigDTO result = systemConfigService.getDefaultGroupRules();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.ruleIds()).isEmpty();
    }

    @Test
    @DisplayName("更新默认群组规则配置 - 配置已存在")
    void testUpdateDefaultGroupRules_ConfigExists() throws Exception {
        // Given
        DefaultRuleConfigDTO config = DefaultRuleConfigDTO.of(Arrays.asList(1L, 2L));
        SystemConfig existingConfig = new SystemConfig();
        existingConfig.setConfigKey(SystemConfigKeys.DEFAULT_GROUP_RULES);

        when(systemConfigMapper.selectByConfigKey(SystemConfigKeys.DEFAULT_GROUP_RULES)).thenReturn(existingConfig);
        when(objectMapper.writeValueAsString(config)).thenReturn("{\"ruleIds\":[1,2]}");
        when(systemConfigMapper.updateConfigValue(anyString(), anyString())).thenReturn(1);

        // When
        systemConfigService.updateDefaultGroupRules(config);

        // Then
        verify(systemConfigMapper, times(1)).updateConfigValue(
                eq(SystemConfigKeys.DEFAULT_GROUP_RULES),
                eq("{\"ruleIds\":[1,2]}")
        );
        verify(systemConfigMapper, never()).insert(any(SystemConfig.class));
    }

    @Test
    @DisplayName("更新默认群组规则配置 - 配置不存在（新建）")
    void testUpdateDefaultGroupRules_ConfigNotExists() throws Exception {
        // Given
        DefaultRuleConfigDTO config = DefaultRuleConfigDTO.of(Arrays.asList(1L, 2L));
        when(systemConfigMapper.selectByConfigKey(SystemConfigKeys.DEFAULT_GROUP_RULES)).thenReturn(null);
        when(objectMapper.writeValueAsString(config)).thenReturn("{\"ruleIds\":[1,2]}");

        // When
        systemConfigService.updateDefaultGroupRules(config);

        // Then
        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(systemConfigMapper, times(1)).insert(captor.capture());
        SystemConfig inserted = captor.getValue();
        assertThat(inserted.getConfigKey()).isEqualTo(SystemConfigKeys.DEFAULT_GROUP_RULES);
        verify(systemConfigMapper, never()).updateConfigValue(anyString(), anyString());
    }

    @Test
    @DisplayName("获取配置 - 从缓存获取")
    void testGetConfig_FromCache() {
        // Given - First call populates cache
        SystemConfig config = new SystemConfig();
        config.setConfigKey("test.key");
        config.setConfigValue(Collections.singletonMap("value", "test"));

        when(systemConfigMapper.selectByConfigKey("test.key")).thenReturn(config);
        when(objectMapper.convertValue(any(), eq(Map.class)))
                .thenReturn(Collections.singletonMap("value", "test"));

        // When - First call
        Map<String, String> result1 = systemConfigService.getConfig("test.key", Map.class);

        // When - Second call (should hit cache)
        Map<String, String> result2 = systemConfigService.getConfig("test.key", Map.class);

        // Then
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        // Mapper should only be called once (first time)
        verify(systemConfigMapper, times(1)).selectByConfigKey("test.key");
    }

    @Test
    @DisplayName("获取配置 - 配置不存在返回默认值")
    void testGetConfig_NotExists_ReturnsDefault() {
        // Given
        when(systemConfigMapper.selectByConfigKey(SystemConfigKeys.DEFAULT_GROUP_RULES)).thenReturn(null);

        // When
        DefaultRuleConfigDTO result = systemConfigService.getConfig(
                SystemConfigKeys.DEFAULT_GROUP_RULES,
                DefaultRuleConfigDTO.class
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.ruleIds()).isEmpty();
    }

    @Test
    @DisplayName("清除配置缓存")
    void testClearCache() {
        // Given
        SystemConfig config = new SystemConfig();
        config.setConfigKey("test.key");
        config.setConfigValue(Collections.singletonMap("value", "test"));

        when(systemConfigMapper.selectByConfigKey("test.key")).thenReturn(config);
        when(objectMapper.convertValue(any(), eq(Map.class)))
                .thenReturn(Collections.singletonMap("value", "test"));

        // When - First call populates cache
        systemConfigService.getConfig("test.key", Map.class);

        // Clear cache
        systemConfigService.clearCache();

        // Second call after cache clear
        systemConfigService.getConfig("test.key", Map.class);

        // Then - Mapper should be called twice (cache was cleared)
        verify(systemConfigMapper, times(2)).selectByConfigKey("test.key");
    }

    @Test
    @DisplayName("获取同步任务配置")
    void testGetSyncTaskConfig() {
        // Given
        SystemConfig config = new SystemConfig();
        config.setConfigKey(SystemConfigKeys.SYNC_TASK_CONFIG);
        Map<String, Object> configValue = new HashMap<>();
        configValue.put("cron", "0 0 */6 * * ?");
        configValue.put("batchSize", 50);
        config.setConfigValue(configValue);

        when(systemConfigMapper.selectByConfigKey(SystemConfigKeys.SYNC_TASK_CONFIG)).thenReturn(config);
        when(objectMapper.convertValue(any(), eq(Map.class))).thenReturn(configValue);

        // When
        Map<String, Object> result = systemConfigService.getSyncTaskConfig(Map.class);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("cron")).isEqualTo("0 0 */6 * * ?");
        assertThat(result.get("batchSize")).isEqualTo(50);
    }

    @Test
    @DisplayName("更新同步任务配置")
    void testUpdateSyncTaskConfig() throws Exception {
        // Given
        Map<String, Object> config = new HashMap<>();
        config.put("cron", "0 0 */12 * * ?");
        config.put("batchSize", 100);

        SystemConfig existingConfig = new SystemConfig();
        existingConfig.setConfigKey(SystemConfigKeys.SYNC_TASK_CONFIG);

        when(systemConfigMapper.selectByConfigKey(SystemConfigKeys.SYNC_TASK_CONFIG)).thenReturn(existingConfig);
        when(objectMapper.writeValueAsString(config)).thenReturn("{\"cron\":\"0 0 */12 * * ?\",\"batchSize\":100}");
        when(systemConfigMapper.updateConfigValue(anyString(), anyString())).thenReturn(1);

        // When
        systemConfigService.updateSyncTaskConfig(config);

        // Then
        verify(systemConfigMapper, times(1)).updateConfigValue(
                eq(SystemConfigKeys.SYNC_TASK_CONFIG),
                anyString()
        );
    }

    @Test
    @DisplayName("获取重试策略配置")
    void testGetRetryPolicyConfig() {
        // Given
        SystemConfig config = new SystemConfig();
        config.setConfigKey(SystemConfigKeys.RETRY_POLICY_CONFIG);
        Map<String, Object> configValue = new HashMap<>();
        configValue.put("maxAttempts", 3);
        configValue.put("backoffDelays", Arrays.asList(30, 120, 300));
        config.setConfigValue(configValue);

        when(systemConfigMapper.selectByConfigKey(SystemConfigKeys.RETRY_POLICY_CONFIG)).thenReturn(config);
        when(objectMapper.convertValue(any(), eq(Map.class))).thenReturn(configValue);

        // When
        Map<String, Object> result = systemConfigService.getRetryPolicyConfig(Map.class);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("maxAttempts")).isEqualTo(3);
        assertThat(result.get("backoffDelays")).isEqualTo(Arrays.asList(30, 120, 300));
    }

    @Test
    @DisplayName("更新重试策略配置")
    void testUpdateRetryPolicyConfig() throws Exception {
        // Given
        Map<String, Object> config = new HashMap<>();
        config.put("maxAttempts", 5);
        config.put("backoffDelays", Arrays.asList(60, 180, 360));

        when(systemConfigMapper.selectByConfigKey(SystemConfigKeys.RETRY_POLICY_CONFIG)).thenReturn(null);
        when(objectMapper.writeValueAsString(config)).thenReturn("{\"maxAttempts\":5}");

        // When
        systemConfigService.updateRetryPolicyConfig(config);

        // Then
        verify(systemConfigMapper, times(1)).insert(any(SystemConfig.class));
    }
}
