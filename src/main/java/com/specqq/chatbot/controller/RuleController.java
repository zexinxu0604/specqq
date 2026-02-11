package com.specqq.chatbot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.specqq.chatbot.common.Result;
import com.specqq.chatbot.common.ResultCode;
import com.specqq.chatbot.entity.MessageRule;
import com.specqq.chatbot.service.RuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 规则管理控制器
 *
 * @author Chatbot Router System
 */
@Slf4j
@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
@Validated
@Tag(name = "规则管理", description = "消息规则的增删改查接口")
public class RuleController {

    private final RuleService ruleService;

    /**
     * 分页查询规则列表
     */
    @GetMapping
    @Operation(summary = "分页查询规则", description = "支持按规则名称、匹配类型、启用状态筛选")
    public Result<Page<MessageRule>> listRules(
        @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") Integer page,
        @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer size,
        @Parameter(description = "规则名称（模糊查询）") @RequestParam(required = false) String name,
        @Parameter(description = "匹配类型：EXACT/CONTAINS/REGEX") @RequestParam(required = false) String matchType,
        @Parameter(description = "启用状态") @RequestParam(required = false) Boolean enabled
    ) {
        log.info("查询规则列表: page={}, size={}, name={}, matchType={}, enabled={}",
            page, size, name, matchType, enabled);

        Page<MessageRule> result = ruleService.listRules(page, size, name, matchType, enabled);
        return Result.success(result);
    }

    /**
     * 根据ID查询规则详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询规则详情", description = "根据规则ID查询详细信息")
    public Result<MessageRule> getRuleById(
        @Parameter(description = "规则ID") @PathVariable Long id
    ) {
        log.info("查询规则详情: id={}", id);

        MessageRule rule = ruleService.getRuleById(id);
        if (rule == null) {
            return Result.error(ResultCode.RULE_NOT_FOUND);
        }

        return Result.success(rule);
    }

    /**
     * 创建规则
     */
    @PostMapping
    @Operation(summary = "创建规则", description = "创建新的消息匹配规则")
    public Result<MessageRule> createRule(
        @Valid @RequestBody MessageRule rule
    ) {
        log.info("创建规则: name={}, matchType={}, pattern={}",
            rule.getName(), rule.getMatchType(), rule.getPattern());

        // 验证规则名称唯一性
        if (ruleService.existsByName(rule.getName())) {
            return Result.error(ResultCode.RULE_NAME_DUPLICATE);
        }

        // 验证正则表达式（如果是正则匹配类型）
        if (MessageRule.MatchType.REGEX.equals(rule.getMatchType())) {
            if (!ruleService.validateRegexPattern(rule.getPattern())) {
                return Result.error(ResultCode.RULE_PATTERN_INVALID, "正则表达式语法错误");
            }
        }

        MessageRule created = ruleService.createRule(rule);
        return Result.success("规则创建成功", created);
    }

    /**
     * 更新规则
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新规则", description = "更新现有规则的信息")
    public Result<MessageRule> updateRule(
        @Parameter(description = "规则ID") @PathVariable Long id,
        @Valid @RequestBody MessageRule rule
    ) {
        log.info("更新规则: id={}, name={}, matchType={}, pattern={}",
            id, rule.getName(), rule.getMatchType(), rule.getPattern());

        // 检查规则是否存在
        MessageRule existing = ruleService.getRuleById(id);
        if (existing == null) {
            return Result.error(ResultCode.RULE_NOT_FOUND);
        }

        // 验证规则名称唯一性（排除自身）
        if (!existing.getName().equals(rule.getName()) && ruleService.existsByName(rule.getName())) {
            return Result.error(ResultCode.RULE_NAME_DUPLICATE);
        }

        // 验证正则表达式
        if (MessageRule.MatchType.REGEX.equals(rule.getMatchType())) {
            if (!ruleService.validateRegexPattern(rule.getPattern())) {
                return Result.error(ResultCode.RULE_PATTERN_INVALID, "正则表达式语法错误");
            }
        }

        rule.setId(id);
        MessageRule updated = ruleService.updateRule(rule);
        return Result.success("规则更新成功", updated);
    }

    /**
     * 删除规则
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除规则", description = "删除指定规则（检查是否被使用）")
    public Result<Void> deleteRule(
        @Parameter(description = "规则ID") @PathVariable Long id
    ) {
        log.info("删除规则: id={}", id);

        // 检查规则是否存在
        MessageRule rule = ruleService.getRuleById(id);
        if (rule == null) {
            return Result.error(ResultCode.RULE_NOT_FOUND);
        }

        // 检查规则是否正在使用
        if (ruleService.isRuleInUse(id)) {
            return Result.error(ResultCode.RULE_IN_USE, "规则正在被群聊使用，请先解除关联");
        }

        ruleService.deleteRule(id);
        return Result.success("规则删除成功", null);
    }

    /**
     * 切换规则启用状态
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "切换规则状态", description = "启用或禁用规则")
    public Result<Void> toggleRuleStatus(
        @Parameter(description = "规则ID") @PathVariable Long id,
        @Parameter(description = "启用状态") @RequestParam Boolean enabled
    ) {
        log.info("切换规则状态: id={}, enabled={}", id, enabled);

        // 检查规则是否存在
        MessageRule rule = ruleService.getRuleById(id);
        if (rule == null) {
            return Result.error(ResultCode.RULE_NOT_FOUND);
        }

        ruleService.toggleRuleStatus(id, enabled);
        return Result.success(enabled ? "规则已启用" : "规则已禁用", null);
    }

    /**
     * 批量删除规则
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除规则", description = "批量删除多个规则")
    public Result<Void> batchDeleteRules(
        @Parameter(description = "规则ID列表") @RequestBody java.util.List<Long> ids
    ) {
        log.info("批量删除规则: ids={}", ids);

        if (ids == null || ids.isEmpty()) {
            return Result.error(ResultCode.BAD_REQUEST, "规则ID列表不能为空");
        }

        // 检查是否有规则正在使用
        for (Long id : ids) {
            if (ruleService.isRuleInUse(id)) {
                MessageRule rule = ruleService.getRuleById(id);
                return Result.error(ResultCode.RULE_IN_USE,
                    String.format("规则「%s」正在被使用，无法删除", rule.getName()));
            }
        }

        int deletedCount = ruleService.batchDeleteRules(ids);
        return Result.success(String.format("成功删除 %d 条规则", deletedCount), null);
    }

    /**
     * 复制规则
     */
    @PostMapping("/{id}/copy")
    @Operation(summary = "复制规则", description = "复制现有规则创建新规则")
    public Result<MessageRule> copyRule(
        @Parameter(description = "源规则ID") @PathVariable Long id,
        @Parameter(description = "新规则名称") @RequestParam String newName
    ) {
        log.info("复制规则: id={}, newName={}", id, newName);

        // 检查源规则是否存在
        MessageRule sourceRule = ruleService.getRuleById(id);
        if (sourceRule == null) {
            return Result.error(ResultCode.RULE_NOT_FOUND);
        }

        // 验证新名称唯一性
        if (ruleService.existsByName(newName)) {
            return Result.error(ResultCode.RULE_NAME_DUPLICATE);
        }

        MessageRule copiedRule = ruleService.copyRule(id, newName);
        return Result.success("规则复制成功", copiedRule);
    }

    /**
     * 验证规则匹配模式
     */
    @PostMapping("/validate-pattern")
    @Operation(summary = "验证匹配模式", description = "验证正则表达式或匹配模式是否有效")
    public Result<Boolean> validatePattern(
        @Parameter(description = "匹配类型") @RequestParam MessageRule.MatchType matchType,
        @Parameter(description = "匹配模式") @RequestParam String pattern
    ) {
        log.info("验证匹配模式: matchType={}, pattern={}", matchType, pattern);

        if (MessageRule.MatchType.REGEX.equals(matchType)) {
            boolean valid = ruleService.validateRegexPattern(pattern);
            if (!valid) {
                return Result.error(ResultCode.RULE_PATTERN_INVALID, "正则表达式语法错误");
            }
        }

        return Result.success("匹配模式有效", true);
    }

    /**
     * 测试规则匹配
     */
    @PostMapping("/test-match")
    @Operation(summary = "测试规则匹配", description = "测试规则是否能匹配指定消息")
    public Result<Boolean> testRuleMatch(
        @Parameter(description = "匹配类型") @RequestParam MessageRule.MatchType matchType,
        @Parameter(description = "匹配模式") @RequestParam String pattern,
        @Parameter(description = "测试消息") @RequestParam String message
    ) {
        log.info("测试规则匹配: matchType={}, pattern={}, message={}",
            matchType, pattern, message);

        boolean matched = ruleService.testRuleMatch(matchType, pattern, message);
        return Result.success(matched ? "匹配成功" : "未匹配", matched);
    }
}
