package com.specqq.chatbot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specqq.chatbot.dto.DefaultRuleConfigDTO;
import com.specqq.chatbot.entity.GroupChat;
import com.specqq.chatbot.entity.MessageRule;
import com.specqq.chatbot.entity.MessageRule.MatchType;
import com.specqq.chatbot.service.DefaultRuleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DefaultRuleController API测试
 *
 * @author Claude Code
 * @since 2026-02-12
 */
@WebMvcTest(DefaultRuleController.class)
@DisplayName("默认规则控制器API测试")
class DefaultRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DefaultRuleService defaultRuleService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/rules/default - 获取默认规则配置")
    void testGetDefaultRuleConfig() throws Exception {
        // Given
        DefaultRuleConfigDTO config = DefaultRuleConfigDTO.of(Arrays.asList(1L, 2L, 3L));
        when(defaultRuleService.getDefaultRuleConfig()).thenReturn(config);

        // When & Then
        mockMvc.perform(get("/api/rules/default")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.ruleIds").isArray())
                .andExpect(jsonPath("$.data.ruleIds.length()").value(3))
                .andExpect(jsonPath("$.data.ruleIds[0]").value(1))
                .andExpect(jsonPath("$.data.ruleIds[1]").value(2))
                .andExpect(jsonPath("$.data.ruleIds[2]").value(3));

        verify(defaultRuleService, times(1)).getDefaultRuleConfig();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/rules/default - 更新默认规则配置")
    void testUpdateDefaultRuleConfig() throws Exception {
        // Given
        DefaultRuleConfigDTO config = DefaultRuleConfigDTO.of(Arrays.asList(1L, 2L));
        when(defaultRuleService.validateRuleIds(any())).thenReturn(Collections.emptyList());
        doNothing().when(defaultRuleService).updateDefaultRuleConfig(any());

        // When & Then
        mockMvc.perform(put("/api/rules/default")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("默认规则配置更新成功"));

        verify(defaultRuleService, times(1)).validateRuleIds(any());
        verify(defaultRuleService, times(1)).updateDefaultRuleConfig(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/rules/default - 包含无效规则ID")
    void testUpdateDefaultRuleConfig_InvalidRuleIds() throws Exception {
        // Given
        DefaultRuleConfigDTO config = DefaultRuleConfigDTO.of(Arrays.asList(1L, 999L));
        when(defaultRuleService.validateRuleIds(any())).thenReturn(Collections.singletonList(999L));

        // When & Then
        mockMvc.perform(put("/api/rules/default")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("无效的规则ID: [999]"));

        verify(defaultRuleService, times(1)).validateRuleIds(any());
        verify(defaultRuleService, never()).updateDefaultRuleConfig(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/rules/default/rules - 获取默认规则列表")
    void testGetDefaultRules() throws Exception {
        // Given
        List<MessageRule> rules = Arrays.asList(
                createRule(1L, "Rule 1", MatchType.CONTAINS),
                createRule(2L, "Rule 2", MatchType.REGEX)
        );
        when(defaultRuleService.getDefaultRules()).thenReturn(rules);

        // When & Then
        mockMvc.perform(get("/api/rules/default/rules")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("Rule 1"))
                .andExpect(jsonPath("$.data[1].name").value("Rule 2"));

        verify(defaultRuleService, times(1)).getDefaultRules();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/rules/default/apply/batch - 批量应用默认规则")
    void testBatchApplyDefaultRules() throws Exception {
        // Given
        List<GroupChat> groups = Arrays.asList(
                createGroup(1L, "Group 1"),
                createGroup(2L, "Group 2")
        );
        when(defaultRuleService.getGroupsWithoutDefaultRules()).thenReturn(groups);
        when(defaultRuleService.batchApplyDefaultRules(any())).thenReturn(4);

        // When & Then
        mockMvc.perform(post("/api/rules/default/apply/batch")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(4))
                .andExpect(jsonPath("$.message").value("成功为 2 个群组绑定了 4 条规则"));

        verify(defaultRuleService, times(1)).getGroupsWithoutDefaultRules();
        verify(defaultRuleService, times(1)).batchApplyDefaultRules(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/rules/default/apply/batch - 所有群组已绑定")
    void testBatchApplyDefaultRules_AllBound() throws Exception {
        // Given
        when(defaultRuleService.getGroupsWithoutDefaultRules()).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(post("/api/rules/default/apply/batch")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(0))
                .andExpect(jsonPath("$.message").value("所有群组已绑定默认规则"));

        verify(defaultRuleService, times(1)).getGroupsWithoutDefaultRules();
        verify(defaultRuleService, never()).batchApplyDefaultRules(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/rules/default/groups/without - 获取未绑定默认规则的群组")
    void testGetGroupsWithoutDefaultRules() throws Exception {
        // Given
        List<GroupChat> groups = Arrays.asList(
                createGroup(1L, "Group 1"),
                createGroup(2L, "Group 2")
        );
        when(defaultRuleService.getGroupsWithoutDefaultRules()).thenReturn(groups);

        // When & Then
        mockMvc.perform(get("/api/rules/default/groups/without")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));

        verify(defaultRuleService, times(1)).getGroupsWithoutDefaultRules();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/rules/default/validate - 验证规则ID（全部有效）")
    void testValidateRuleIds_AllValid() throws Exception {
        // Given
        List<Long> ruleIds = Arrays.asList(1L, 2L, 3L);
        when(defaultRuleService.validateRuleIds(ruleIds)).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(post("/api/rules/default/validate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ruleIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0))
                .andExpect(jsonPath("$.message").value("所有规则ID有效"));

        verify(defaultRuleService, times(1)).validateRuleIds(ruleIds);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/rules/default/validate - 验证规则ID（部分无效）")
    void testValidateRuleIds_PartialInvalid() throws Exception {
        // Given
        List<Long> ruleIds = Arrays.asList(1L, 999L, 2L);
        when(defaultRuleService.validateRuleIds(ruleIds)).thenReturn(Collections.singletonList(999L));

        // When & Then
        mockMvc.perform(post("/api/rules/default/validate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ruleIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("发现无效的规则ID: [999]"));

        verify(defaultRuleService, times(1)).validateRuleIds(ruleIds);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("PUT /api/rules/default - 非管理员访问被拒绝")
    void testUpdateDefaultRuleConfig_Forbidden() throws Exception {
        // Given
        DefaultRuleConfigDTO config = DefaultRuleConfigDTO.of(Arrays.asList(1L, 2L));

        // When & Then
        mockMvc.perform(put("/api/rules/default")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isForbidden());

        verify(defaultRuleService, never()).updateDefaultRuleConfig(any());
    }

    // Helper methods
    private MessageRule createRule(Long id, String name, MatchType matchType) {
        MessageRule rule = new MessageRule();
        rule.setId(id);
        rule.setName(name);
        rule.setMatchType(matchType);
        rule.setPattern("test");
        rule.setResponseTemplate("response");
        rule.setPriority(100);
        rule.setEnabled(true);
        return rule;
    }

    private GroupChat createGroup(Long id, String groupName) {
        GroupChat group = new GroupChat();
        group.setId(id);
        group.setGroupId(String.valueOf(id));
        group.setGroupName(groupName);
        group.setEnabled(true);
        group.setActive(true);
        return group;
    }
}
