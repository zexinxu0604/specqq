package com.specqq.chatbot.integration;

import com.specqq.chatbot.constant.SystemConfigKeys;
import com.specqq.chatbot.dto.DefaultRuleConfigDTO;
import com.specqq.chatbot.entity.SystemConfig;
import com.specqq.chatbot.mapper.SystemConfigMapper;
import com.specqq.chatbot.service.SystemConfigService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SystemConfigService 集成测试
 *
 * @author Claude Code
 * @since 2026-02-12
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("系统配置服务集成测试")
class SystemConfigServiceIntegrationTest {

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Test
    @DisplayName("更新并获取默认群组规则配置")
    void testUpdateAndGetDefaultGroupRules() {
        // Given
        DefaultRuleConfigDTO config = DefaultRuleConfigDTO.of(Arrays.asList(1L, 2L, 3L));

        // When
        systemConfigService.updateDefaultGroupRules(config);
        DefaultRuleConfigDTO retrieved = systemConfigService.getDefaultGroupRules();

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.ruleIds()).hasSize(3);
        assertThat(retrieved.ruleIds()).containsExactly(1L, 2L, 3L);

        // 验证数据库
        SystemConfig dbConfig = systemConfigMapper.selectByConfigKey(SystemConfigKeys.DEFAULT_GROUP_RULES);
        assertThat(dbConfig).isNotNull();
        assertThat(dbConfig.getConfigKey()).isEqualTo(SystemConfigKeys.DEFAULT_GROUP_RULES);
    }

    @Test
    @DisplayName("获取不存在的配置 - 返回默认值")
    void testGetConfig_NotExists_ReturnsDefault() {
        // When
        DefaultRuleConfigDTO config = systemConfigService.getDefaultGroupRules();

        // Then - 应该返回空配置
        assertThat(config).isNotNull();
        assertThat(config.ruleIds()).isEmpty();
    }

    @Test
    @DisplayName("更新同步任务配置")
    void testUpdateSyncTaskConfig() {
        // Given
        Map<String, Object> config = new HashMap<>();
        config.put("cron", "0 0 */12 * * ?");
        config.put("batchSize", 100);
        config.put("timeoutSeconds", 20);

        // When
        systemConfigService.updateSyncTaskConfig(config);
        Map<String, Object> retrieved = systemConfigService.getSyncTaskConfig(Map.class);

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.get("cron")).isEqualTo("0 0 */12 * * ?");
        assertThat(retrieved.get("batchSize")).isEqualTo(100);
        assertThat(retrieved.get("timeoutSeconds")).isEqualTo(20);
    }

    @Test
    @DisplayName("更新重试策略配置")
    void testUpdateRetryPolicyConfig() {
        // Given
        Map<String, Object> config = new HashMap<>();
        config.put("maxAttempts", 5);
        config.put("backoffDelays", Arrays.asList(60, 180, 360));

        // When
        systemConfigService.updateRetryPolicyConfig(config);
        Map<String, Object> retrieved = systemConfigService.getRetryPolicyConfig(Map.class);

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.get("maxAttempts")).isEqualTo(5);
        assertThat(retrieved.get("backoffDelays")).isEqualTo(Arrays.asList(60, 180, 360));
    }

    @Test
    @DisplayName("通用配置获取和更新")
    void testGenericConfigGetAndUpdate() {
        // Given
        String testKey = "test_config_key";
        Map<String, Object> testConfig = new HashMap<>();
        testConfig.put("setting1", "value1");
        testConfig.put("setting2", 42);

        // When - 更新配置
        systemConfigService.updateConfig(testKey, testConfig);

        // Then - 获取配置
        Map<String, Object> retrieved = systemConfigService.getConfig(testKey, Map.class);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.get("setting1")).isEqualTo("value1");
        assertThat(retrieved.get("setting2")).isEqualTo(42);
    }

    @Test
    @DisplayName("清除缓存后重新从数据库加载")
    void testClearCache_ReloadFromDatabase() {
        // Given - 先设置配置
        DefaultRuleConfigDTO config = DefaultRuleConfigDTO.of(Arrays.asList(1L, 2L));
        systemConfigService.updateDefaultGroupRules(config);

        // 第一次获取（加载到缓存）
        DefaultRuleConfigDTO firstRetrieval = systemConfigService.getDefaultGroupRules();
        assertThat(firstRetrieval.ruleIds()).hasSize(2);

        // 直接修改数据库（绕过服务层）
        SystemConfig dbConfig = systemConfigMapper.selectByConfigKey(SystemConfigKeys.DEFAULT_GROUP_RULES);
        Map<String, Object> newValue = new HashMap<>();
        newValue.put("ruleIds", Arrays.asList(3, 4, 5));
        dbConfig.setConfigValue(newValue);
        systemConfigMapper.updateById(dbConfig);

        // 清除缓存前 - 应该还是旧值（从缓存获取）
        DefaultRuleConfigDTO beforeClear = systemConfigService.getDefaultGroupRules();
        assertThat(beforeClear.ruleIds()).hasSize(2); // Still cached

        // When - 清除缓存
        systemConfigService.clearCache();

        // Then - 清除缓存后应该从数据库重新加载新值
        DefaultRuleConfigDTO afterClear = systemConfigService.getDefaultGroupRules();
        assertThat(afterClear.ruleIds()).hasSize(3);
        assertThat(afterClear.ruleIds()).containsExactly(3L, 4L, 5L);
    }

    @Test
    @DisplayName("配置更新后自动更新缓存")
    void testUpdateConfig_AutoUpdateCache() {
        // Given
        DefaultRuleConfigDTO config1 = DefaultRuleConfigDTO.of(Arrays.asList(1L, 2L));
        systemConfigService.updateDefaultGroupRules(config1);

        // 第一次获取
        DefaultRuleConfigDTO retrieved1 = systemConfigService.getDefaultGroupRules();
        assertThat(retrieved1.ruleIds()).hasSize(2);

        // When - 更新配置
        DefaultRuleConfigDTO config2 = DefaultRuleConfigDTO.of(Arrays.asList(3L, 4L, 5L));
        systemConfigService.updateDefaultGroupRules(config2);

        // Then - 立即获取应该是新值（缓存已更新）
        DefaultRuleConfigDTO retrieved2 = systemConfigService.getDefaultGroupRules();
        assertThat(retrieved2.ruleIds()).hasSize(3);
        assertThat(retrieved2.ruleIds()).containsExactly(3L, 4L, 5L);
    }

    @Test
    @DisplayName("配置不存在时插入新记录")
    void testUpdateConfig_InsertWhenNotExists() {
        // Given
        String newKey = "new_test_config";
        Map<String, Object> newConfig = new HashMap<>();
        newConfig.put("value", "test");

        // When
        systemConfigService.updateConfig(newKey, newConfig);

        // Then - 验证数据库
        SystemConfig dbConfig = systemConfigMapper.selectByConfigKey(newKey);
        assertThat(dbConfig).isNotNull();
        assertThat(dbConfig.getConfigKey()).isEqualTo(newKey);
        assertThat(dbConfig.getConfigValue()).isNotNull();
    }

    @Test
    @DisplayName("配置存在时更新记录")
    void testUpdateConfig_UpdateWhenExists() {
        // Given - 先插入配置
        String key = "existing_config";
        Map<String, Object> initialConfig = new HashMap<>();
        initialConfig.put("value", "initial");
        systemConfigService.updateConfig(key, initialConfig);

        // When - 更新配置
        Map<String, Object> updatedConfig = new HashMap<>();
        updatedConfig.put("value", "updated");
        systemConfigService.updateConfig(key, updatedConfig);

        // Then
        Map<String, Object> retrieved = systemConfigService.getConfig(key, Map.class);
        assertThat(retrieved.get("value")).isEqualTo("updated");
    }
}
