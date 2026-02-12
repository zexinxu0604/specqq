package com.specqq.chatbot.controller;

import com.specqq.chatbot.common.Result;
import com.specqq.chatbot.common.enums.OnErrorPolicy;
import com.specqq.chatbot.common.enums.RuleStatus;
import com.specqq.chatbot.entity.MessageRule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统配置控制器
 * 提供前端所需的枚举值、配置选项等
 *
 * @author Chatbot Router System
 */
@Slf4j
@RestController
@RequestMapping("/api/system/config")
@RequiredArgsConstructor
@Tag(name = "系统配置", description = "提供系统配置、枚举值等信息")
public class SystemConfigController {

    /**
     * 获取所有匹配类型枚举值
     */
    @GetMapping("/match-types")
    @Operation(summary = "获取匹配类型列表", description = "返回所有可用的规则匹配类型")
    public Result<List<EnumOption>> getMatchTypes() {
        List<EnumOption> matchTypes = Arrays.stream(MessageRule.MatchType.values())
            .filter(type -> type != MessageRule.MatchType.STATISTICS) // 过滤掉内部使用的类型
            .map(type -> {
                EnumOption option = new EnumOption();
                option.setValue(type.name());
                option.setLabel(getMatchTypeLabel(type));
                option.setDescription(getMatchTypeDescription(type));
                return option;
            })
            .collect(Collectors.toList());

        return Result.success("获取匹配类型成功", matchTypes);
    }

    /**
     * 获取所有规则状态枚举值
     */
    @GetMapping("/rule-statuses")
    @Operation(summary = "获取规则状态列表", description = "返回所有可用的规则状态")
    public Result<List<EnumOption>> getRuleStatuses() {
        List<EnumOption> statuses = Arrays.stream(RuleStatus.values())
            .map(status -> {
                EnumOption option = new EnumOption();
                option.setValue(status.name());
                option.setLabel(getRuleStatusLabel(status));
                option.setDescription(getRuleStatusDescription(status));
                return option;
            })
            .collect(Collectors.toList());

        return Result.success("获取规则状态成功", statuses);
    }

    /**
     * 获取所有错误处理策略枚举值
     */
    @GetMapping("/error-policies")
    @Operation(summary = "获取错误处理策略列表", description = "返回所有可用的错误处理策略")
    public Result<List<EnumOption>> getErrorPolicies() {
        List<EnumOption> policies = Arrays.stream(OnErrorPolicy.values())
            .map(policy -> {
                EnumOption option = new EnumOption();
                option.setValue(policy.name());
                option.setLabel(getErrorPolicyLabel(policy));
                option.setDescription(getErrorPolicyDescription(policy));
                return option;
            })
            .collect(Collectors.toList());

        return Result.success("获取错误处理策略成功", policies);
    }

    /**
     * 获取所有系统配置
     * 一次性返回所有枚举值和配置选项
     */
    @GetMapping("/all")
    @Operation(summary = "获取所有系统配置", description = "一次性返回所有枚举值和配置选项")
    public Result<SystemConfig> getAllConfig() {
        SystemConfig config = new SystemConfig();
        config.setMatchTypes(getMatchTypes().getData());
        config.setRuleStatuses(getRuleStatuses().getData());
        config.setErrorPolicies(getErrorPolicies().getData());

        return Result.success("获取系统配置成功", config);
    }

    // ==================== 辅助方法 ====================

    private String getMatchTypeLabel(MessageRule.MatchType type) {
        return switch (type) {
            case EXACT -> "完全匹配";
            case CONTAINS -> "包含匹配";
            case REGEX -> "正则表达式";
            case PREFIX -> "前缀匹配";
            case SUFFIX -> "后缀匹配";
            case STATISTICS -> "消息统计";
        };
    }

    private String getMatchTypeDescription(MessageRule.MatchType type) {
        return switch (type) {
            case EXACT -> "消息内容必须完全相同（区分大小写）";
            case CONTAINS -> "消息内容包含指定关键词（不区分大小写）";
            case REGEX -> "使用正则表达式进行模式匹配";
            case PREFIX -> "消息以指定前缀开头";
            case SUFFIX -> "消息以指定后缀结尾";
            case STATISTICS -> "自动匹配所有消息，用于统计分析";
        };
    }

    private String getRuleStatusLabel(RuleStatus status) {
        return switch (status) {
            case ENABLED -> "启用";
            case DISABLED -> "禁用";
            case MAINTENANCE -> "维护中";
        };
    }

    private String getRuleStatusDescription(RuleStatus status) {
        return switch (status) {
            case ENABLED -> "规则正常运行，会匹配消息";
            case DISABLED -> "规则已禁用，不会匹配消息";
            case MAINTENANCE -> "规则维护中，暂时不可用";
        };
    }

    private String getErrorPolicyLabel(OnErrorPolicy policy) {
        return switch (policy) {
            case STOP -> "停止执行";
            case CONTINUE -> "继续执行";
            case LOG_ONLY -> "仅记录日志";
        };
    }

    private String getErrorPolicyDescription(OnErrorPolicy policy) {
        return switch (policy) {
            case STOP -> "遇到错误时停止执行后续规则";
            case CONTINUE -> "遇到错误时继续执行后续规则";
            case LOG_ONLY -> "仅记录错误日志，不影响执行";
        };
    }

    // ==================== DTO类 ====================

    /**
     * 枚举选项
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnumOption {
        /**
         * 枚举值（用于API传输）
         */
        private String value;

        /**
         * 显示标签（用于UI展示）
         */
        private String label;

        /**
         * 详细描述
         */
        private String description;
    }

    /**
     * 系统配置
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemConfig {
        /**
         * 匹配类型列表
         */
        private List<EnumOption> matchTypes;

        /**
         * 规则状态列表
         */
        private List<EnumOption> ruleStatuses;

        /**
         * 错误处理策略列表
         */
        private List<EnumOption> errorPolicies;
    }
}
