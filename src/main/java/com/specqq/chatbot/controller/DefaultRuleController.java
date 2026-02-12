package com.specqq.chatbot.controller;

import com.specqq.chatbot.common.Result;
import com.specqq.chatbot.dto.DefaultRuleConfigDTO;
import com.specqq.chatbot.entity.GroupChat;
import com.specqq.chatbot.entity.MessageRule;
import com.specqq.chatbot.service.DefaultRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 默认规则控制器
 * 提供默认规则配置管理和批量绑定接口
 *
 * @author Claude Code
 * @since 2026-02-12
 */
@Slf4j
@RestController
@RequestMapping("/api/rules/default")
@RequiredArgsConstructor
@Tag(name = "默认规则管理", description = "默认规则配置相关接口")
public class DefaultRuleController {

    private final DefaultRuleService defaultRuleService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取默认规则配置", description = "查询当前系统的默认规则配置")
    public Result<DefaultRuleConfigDTO> getDefaultRuleConfig() {
        log.info("查询默认规则配置");
        DefaultRuleConfigDTO config = defaultRuleService.getDefaultRuleConfig();
        return Result.success(config);
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新默认规则配置", description = "更新系统的默认规则配置，新群组将自动绑定这些规则")
    public Result<Void> updateDefaultRuleConfig(
            @Valid @RequestBody DefaultRuleConfigDTO config) {
        log.info("更新默认规则配置: ruleCount={}", config.getRuleCount());

        // 验证规则ID
        List<Long> invalidRuleIds = defaultRuleService.validateRuleIds(config.ruleIds());
        if (!invalidRuleIds.isEmpty()) {
            return Result.error("无效的规则ID: " + invalidRuleIds);
        }

        defaultRuleService.updateDefaultRuleConfig(config);
        return Result.success("默认规则配置更新成功", null);
    }

    @GetMapping("/rules")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取默认规则列表", description = "查询默认规则的详细信息")
    public Result<List<MessageRule>> getDefaultRules() {
        log.info("查询默认规则列表");
        List<MessageRule> rules = defaultRuleService.getDefaultRules();
        return Result.success(rules);
    }

    @PostMapping("/apply/{groupId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "为群组应用默认规则", description = "为指定群组绑定默认规则")
    public Result<Integer> applyDefaultRulesToGroup(
            @Parameter(description = "群组ID")
            @PathVariable Long groupId) {
        log.info("为群组应用默认规则: groupId={}", groupId);
        // TODO: 需要先查询群组实体
        return Result.error("功能开发中");
    }

    @PostMapping("/apply/batch")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "批量应用默认规则", description = "为未绑定默认规则的群组批量绑定默认规则")
    public Result<Integer> batchApplyDefaultRules() {
        log.info("批量应用默认规则");
        List<GroupChat> groupsWithoutDefaultRules = defaultRuleService.getGroupsWithoutDefaultRules();
        if (groupsWithoutDefaultRules.isEmpty()) {
            return Result.success("所有群组已绑定默认规则", 0);
        }

        Integer totalBindCount = defaultRuleService.batchApplyDefaultRules(groupsWithoutDefaultRules);
        return Result.success("成功为 " + groupsWithoutDefaultRules.size() +
                " 个群组绑定了 " + totalBindCount + " 条规则", totalBindCount);
    }

    @GetMapping("/groups/without")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取未绑定默认规则的群组", description = "查询未绑定默认规则的群组列表")
    public Result<List<GroupChat>> getGroupsWithoutDefaultRules() {
        log.info("查询未绑定默认规则的群组");
        List<GroupChat> groups = defaultRuleService.getGroupsWithoutDefaultRules();
        return Result.success(groups);
    }

    @PostMapping("/validate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "验证规则ID", description = "验证规则ID列表是否全部有效")
    public Result<List<Long>> validateRuleIds(
            @RequestBody List<Long> ruleIds) {
        log.info("验证规则ID: count={}", ruleIds.size());
        List<Long> invalidRuleIds = defaultRuleService.validateRuleIds(ruleIds);
        if (invalidRuleIds.isEmpty()) {
            return Result.success("所有规则ID有效", invalidRuleIds);
        } else {
            return Result.error("发现无效的规则ID: " + invalidRuleIds);
        }
    }
}
