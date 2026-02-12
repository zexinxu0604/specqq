package com.specqq.chatbot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.specqq.chatbot.common.Result;
import com.specqq.chatbot.common.ResultCode;
import com.specqq.chatbot.dto.RuleCreateDTO;
import com.specqq.chatbot.dto.RuleUpdateDTO;
import com.specqq.chatbot.entity.MessageRule;
import com.specqq.chatbot.entity.RulePolicy;
import com.specqq.chatbot.service.PolicyService;
import com.specqq.chatbot.service.RuleService;
import com.specqq.chatbot.vo.RuleDetailVO;
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
    private final PolicyService policyService;

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
    @Operation(summary = "查询规则详情", description = "根据规则ID查询详细信息，包含策略配置")
    public Result<RuleDetailVO> getRuleById(
        @Parameter(description = "规则ID") @PathVariable Long id
    ) {
        log.info("查询规则详情: id={}", id);

        RuleService.RuleWithPolicy ruleWithPolicy = ruleService.getRuleWithPolicy(id);
        if (ruleWithPolicy == null || ruleWithPolicy.rule() == null) {
            return Result.error(ResultCode.RULE_NOT_FOUND);
        }

        RuleDetailVO vo = buildRuleDetailVO(ruleWithPolicy.rule(), ruleWithPolicy.policy());
        return Result.success(vo);
    }

    /**
     * 创建规则
     */
    @PostMapping
    @Operation(summary = "创建规则", description = "创建新的消息匹配规则")
    public Result<MessageRule> createRule(
        @Valid @RequestBody RuleCreateDTO dto
    ) {
        log.info("创建规则: name={}, matchType={}, pattern={}",
            dto.getRuleName(), dto.getMatchType(), dto.getPattern());

        // 构建 MessageRule 实体
        MessageRule rule = new MessageRule();
        rule.setName(dto.getRuleName());
        rule.setDescription(dto.getDescription());
        // 转换 MatchType 枚举
        rule.setMatchType(MessageRule.MatchType.valueOf(dto.getMatchType().name()));
        rule.setPattern(dto.getPattern());
        rule.setResponseTemplate(dto.getResponseTemplate());
        rule.setPriority(dto.getPriority());
        rule.setHandlerConfig(dto.getHandlerConfig());
        rule.setEnabled(dto.getEnabled());

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

        // 创建规则
        MessageRule created = ruleService.createRule(rule);

        // 保存 policy（如果提供）
        if (dto.getPolicy() != null) {
            try {
                RulePolicy policy = convertPolicyDTO(dto.getPolicy(), created.getId());
                policyService.saveOrUpdatePolicy(policy);
                log.info("Policy saved for rule: ruleId={}", created.getId());
            } catch (Exception e) {
                log.error("Failed to save policy for rule: ruleId={}", created.getId(), e);
                // Policy 保存失败不影响规则创建
            }
        }

        return Result.success("规则创建成功", created);
    }

    /**
     * 转换 PolicyDTO 为 RulePolicy 实体
     */
    private RulePolicy convertPolicyDTO(RuleCreateDTO.PolicyDTO dto, Long ruleId) {
        RulePolicy policy = new RulePolicy();
        policy.setRuleId(ruleId);
        policy.setScope(dto.getScope());

        // 直接设置 List（MyBatis-Plus 会自动使用 JacksonTypeHandler 转换）
        policy.setWhitelist(dto.getWhitelist());
        policy.setBlacklist(dto.getBlacklist());

        policy.setRateLimitEnabled(dto.getRateLimitEnabled());
        policy.setRateLimitMaxRequests(dto.getRateLimitMaxRequests());
        policy.setRateLimitWindowSeconds(dto.getRateLimitWindowSeconds());

        policy.setTimeWindowEnabled(dto.getTimeWindowEnabled());
        policy.setTimeWindowStart(dto.getTimeWindowStart() != null
            ? java.time.LocalTime.parse(dto.getTimeWindowStart()) : null);
        policy.setTimeWindowEnd(dto.getTimeWindowEnd() != null
            ? java.time.LocalTime.parse(dto.getTimeWindowEnd()) : null);
        policy.setTimeWindowWeekdays(dto.getTimeWindowWeekdays());

        policy.setRoleEnabled(dto.getRoleEnabled());
        policy.setAllowedRoles(dto.getAllowedRoles());

        policy.setCooldownEnabled(dto.getCooldownEnabled());
        policy.setCooldownSeconds(dto.getCooldownSeconds());

        return policy;
    }

    /**
     * 更新规则
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新规则", description = "更新现有规则的信息")
    public Result<MessageRule> updateRule(
        @Parameter(description = "规则ID") @PathVariable Long id,
        @Valid @RequestBody RuleUpdateDTO dto
    ) {
        log.info("更新规则: id={}, name={}, matchType={}, pattern={}",
            id, dto.getRuleName(), dto.getMatchType(), dto.getPattern());

        // 检查规则是否存在
        MessageRule existing = ruleService.getRuleById(id);
        if (existing == null) {
            return Result.error(ResultCode.RULE_NOT_FOUND);
        }

        // 更新规则字段（只更新提供的字段）
        if (dto.getRuleName() != null) {
            // 验证规则名称唯一性（排除自身）
            if (!existing.getName().equals(dto.getRuleName()) && ruleService.existsByName(dto.getRuleName())) {
                return Result.error(ResultCode.RULE_NAME_DUPLICATE);
            }
            existing.setName(dto.getRuleName());
        }
        if (dto.getDescription() != null) {
            existing.setDescription(dto.getDescription());
        }
        if (dto.getMatchType() != null) {
            // 转换 MatchType 枚举
            existing.setMatchType(MessageRule.MatchType.valueOf(dto.getMatchType().name()));
        }
        if (dto.getPattern() != null) {
            existing.setPattern(dto.getPattern());
        }
        if (dto.getPriority() != null) {
            existing.setPriority(dto.getPriority());
        }
        if (dto.getResponseTemplate() != null) {
            existing.setResponseTemplate(dto.getResponseTemplate());
        }
        // 处理 handlerConfig：支持 null 或空字符串来清除配置
        if (dto.getHandlerConfig() != null) {
            // 如果是空字符串，设置为 null 以清除配置
            String handlerConfig = dto.getHandlerConfig().trim().isEmpty() ? null : dto.getHandlerConfig();
            existing.setHandlerConfig(handlerConfig);
        }
        if (dto.getEnabled() != null) {
            existing.setEnabled(dto.getEnabled());
        }

        // 验证正则表达式
        if (MessageRule.MatchType.REGEX.equals(existing.getMatchType())) {
            if (!ruleService.validateRegexPattern(existing.getPattern())) {
                return Result.error(ResultCode.RULE_PATTERN_INVALID, "正则表达式语法错误");
            }
        }

        MessageRule updated = ruleService.updateRule(existing);

        // 更新 policy（如果提供）
        if (dto.getPolicy() != null) {
            try {
                RulePolicy policy = convertUpdatePolicyDTO(dto.getPolicy(), id);
                policyService.saveOrUpdatePolicy(policy);
                log.info("Policy updated for rule: ruleId={}", id);
            } catch (Exception e) {
                log.error("Failed to update policy for rule: ruleId={}", id, e);
                // Policy 更新失败不影响规则更新
            }
        }

        return Result.success("规则更新成功", updated);
    }

    /**
     * 转换 RuleUpdateDTO.PolicyDTO 为 RulePolicy 实体（用于更新）
     */
    private RulePolicy convertUpdatePolicyDTO(RuleUpdateDTO.PolicyDTO dto, Long ruleId) {
        // 先获取现有 policy（如果存在）
        RulePolicy policy = policyService.getPolicyByRuleId(ruleId);
        if (policy == null) {
            policy = new RulePolicy();
            policy.setRuleId(ruleId);
        }

        // 只更新提供的字段
        if (dto.getScope() != null) {
            policy.setScope(dto.getScope());
        }
        if (dto.getWhitelist() != null) {
            policy.setWhitelist(dto.getWhitelist());
        }
        if (dto.getBlacklist() != null) {
            policy.setBlacklist(dto.getBlacklist());
        }
        if (dto.getRateLimitEnabled() != null) {
            policy.setRateLimitEnabled(dto.getRateLimitEnabled());
        }
        if (dto.getRateLimitMaxRequests() != null) {
            policy.setRateLimitMaxRequests(dto.getRateLimitMaxRequests());
        }
        if (dto.getRateLimitWindowSeconds() != null) {
            policy.setRateLimitWindowSeconds(dto.getRateLimitWindowSeconds());
        }
        if (dto.getTimeWindowEnabled() != null) {
            policy.setTimeWindowEnabled(dto.getTimeWindowEnabled());
        }
        if (dto.getTimeWindowStart() != null) {
            policy.setTimeWindowStart(java.time.LocalTime.parse(dto.getTimeWindowStart()));
        }
        if (dto.getTimeWindowEnd() != null) {
            policy.setTimeWindowEnd(java.time.LocalTime.parse(dto.getTimeWindowEnd()));
        }
        if (dto.getTimeWindowWeekdays() != null) {
            policy.setTimeWindowWeekdays(dto.getTimeWindowWeekdays());
        }
        if (dto.getRoleEnabled() != null) {
            policy.setRoleEnabled(dto.getRoleEnabled());
        }
        if (dto.getAllowedRoles() != null) {
            policy.setAllowedRoles(dto.getAllowedRoles());
        }
        if (dto.getCooldownEnabled() != null) {
            policy.setCooldownEnabled(dto.getCooldownEnabled());
        }
        if (dto.getCooldownSeconds() != null) {
            policy.setCooldownSeconds(dto.getCooldownSeconds());
        }

        return policy;
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
    public Result<com.specqq.chatbot.dto.ValidatePatternResponseDTO> validatePattern(
        @Parameter(description = "匹配类型") @RequestParam MessageRule.MatchType matchType,
        @Parameter(description = "匹配模式") @RequestParam String pattern
    ) {
        log.info("验证匹配模式: matchType={}, pattern={}", matchType, pattern);

        if (MessageRule.MatchType.REGEX.equals(matchType)) {
            boolean valid = ruleService.validateRegexPattern(pattern);
            if (!valid) {
                return Result.success(
                    com.specqq.chatbot.dto.ValidatePatternResponseDTO.error("✗ 正则表达式语法错误")
                );
            }
        }

        return Result.success(
            com.specqq.chatbot.dto.ValidatePatternResponseDTO.success("✓ 正则表达式语法正确")
        );
    }

    /**
     * 测试规则匹配
     */
    @PostMapping("/test-match")
    @Operation(summary = "测试规则匹配", description = "测试规则是否能匹配指定消息")
    public Result<TestRuleResult> testRuleMatch(@Valid @RequestBody TestRuleRequest request) {
        log.info("测试规则匹配: matchType={}, pattern={}, message={}",
            request.getMatchType(), request.getMatchPattern(), request.getTestMessage());

        boolean matched = ruleService.testRuleMatch(
            request.getMatchType(),
            request.getMatchPattern(),
            request.getTestMessage()
        );

        TestRuleResult result = new TestRuleResult();
        result.setMatched(matched);
        result.setMessage(matched ? "✓ 消息匹配成功" : "✗ 消息未匹配");

        return Result.success(result.getMessage(), result);
    }

    /**
     * 检查规则名称唯一性
     */
    @GetMapping("/check-name")
    @Operation(summary = "检查规则名称唯一性", description = "用于前端表单异步验证规则名称是否已存在")
    public Result<java.util.Map<String, Boolean>> checkNameUnique(
        @Parameter(description = "规则名称") @RequestParam String name,
        @Parameter(description = "排除的规则ID（编辑时使用）") @RequestParam(required = false) Long excludeId
    ) {
        log.debug("检查规则名称唯一性: name={}, excludeId={}", name, excludeId);

        boolean exists;
        if (excludeId != null) {
            // 编辑时，排除自身
            MessageRule existing = ruleService.getRuleById(excludeId);
            if (existing != null && existing.getName().equals(name)) {
                // 名称未改变，视为唯一
                exists = false;
            } else {
                exists = ruleService.existsByName(name);
            }
        } else {
            // 新建时，直接检查是否存在
            exists = ruleService.existsByName(name);
        }

        java.util.Map<String, Boolean> result = new java.util.HashMap<>();
        result.put("unique", !exists);

        return Result.success(result);
    }

    /**
     * 创建规则（新版，支持策略）
     */
    @PostMapping("/v2")
    @Operation(summary = "创建规则（V2）", description = "创建新的消息匹配规则，支持策略配置")
    public Result<RuleDetailVO> createRuleV2(
        @Valid @RequestBody RuleCreateDTO dto
    ) {
        log.info("创建规则 V2: ruleName={}, priority={}", dto.getRuleName(), dto.getPriority());

        // 验证规则名称唯一性
        if (ruleService.existsByName(dto.getRuleName())) {
            return Result.error(ResultCode.RULE_NAME_DUPLICATE);
        }

        // 构建规则实体
        MessageRule rule = new MessageRule();
        rule.setName(dto.getRuleName());
        rule.setDescription(dto.getDescription());
        rule.setMatchType(convertToEntityMatchType(dto.getMatchType()));
        rule.setPattern(dto.getPattern());
        rule.setPriority(dto.getPriority());
        rule.setHandlerConfig(dto.getHandlerConfig());
        rule.setOnErrorPolicy(dto.getOnErrorPolicy());
        rule.setEnabled(dto.getEnabled());

        // 验证正则表达式
        if (MessageRule.MatchType.REGEX.equals(dto.getMatchType())) {
            if (!ruleService.validateRegexPattern(dto.getPattern())) {
                return Result.error(ResultCode.RULE_PATTERN_INVALID, "正则表达式语法错误");
            }
        }

        // 构建策略实体
        RulePolicy policy = null;
        if (dto.getPolicy() != null) {
            policy = convertPolicyDTOToEntity(dto.getPolicy());
        }

        // 创建规则和策略
        MessageRule created = ruleService.createRuleWithPolicy(rule, policy);

        // 构建响应 VO
        RuleDetailVO vo = buildRuleDetailVO(created, policy);

        return Result.success("规则创建成功", vo);
    }

    /**
     * 更新规则（新版，支持策略）
     */
    @PutMapping("/v2/{id}")
    @Operation(summary = "更新规则（V2）", description = "更新现有规则，支持策略配置")
    public Result<RuleDetailVO> updateRuleV2(
        @Parameter(description = "规则ID") @PathVariable Long id,
        @Valid @RequestBody RuleUpdateDTO dto
    ) {
        log.info("更新规则 V2: id={}, ruleName={}", id, dto.getRuleName());

        // 检查规则是否存在
        MessageRule existing = ruleService.getRuleById(id);
        if (existing == null) {
            return Result.error(ResultCode.RULE_NOT_FOUND);
        }

        // 验证规则名称唯一性（排除自身）
        if (dto.getRuleName() != null && !existing.getName().equals(dto.getRuleName())) {
            if (ruleService.existsByName(dto.getRuleName())) {
                return Result.error(ResultCode.RULE_NAME_DUPLICATE);
            }
        }

        // 更新规则字段（只更新提供的字段）
        if (dto.getRuleName() != null) {
            existing.setName(dto.getRuleName());
        }
        if (dto.getDescription() != null) {
            existing.setDescription(dto.getDescription());
        }
        if (dto.getMatchType() != null) {
            existing.setMatchType(convertToEntityMatchType(dto.getMatchType()));
        }
        if (dto.getPattern() != null) {
            existing.setPattern(dto.getPattern());
        }
        if (dto.getPriority() != null) {
            existing.setPriority(dto.getPriority());
        }
        if (dto.getHandlerConfig() != null) {
            existing.setHandlerConfig(dto.getHandlerConfig());
        }
        if (dto.getOnErrorPolicy() != null) {
            existing.setOnErrorPolicy(dto.getOnErrorPolicy());
        }
        if (dto.getEnabled() != null) {
            existing.setEnabled(dto.getEnabled());
        }

        // 验证正则表达式
        if (MessageRule.MatchType.REGEX.equals(existing.getMatchType())) {
            if (!ruleService.validateRegexPattern(existing.getPattern())) {
                return Result.error(ResultCode.RULE_PATTERN_INVALID, "正则表达式语法错误");
            }
        }

        // 构建策略实体
        RulePolicy policy = null;
        if (dto.getPolicy() != null) {
            policy = convertUpdatePolicyDTOToEntity(dto.getPolicy());
        }

        // 更新规则和策略
        MessageRule updated = ruleService.updateRuleWithPolicy(existing, policy);

        // 构建响应 VO
        RulePolicy updatedPolicy = policyService.getPolicyByRuleId(id);
        RuleDetailVO vo = buildRuleDetailVO(updated, updatedPolicy);

        return Result.success("规则更新成功", vo);
    }

    /**
     * 获取规则详情（新版，包含策略）
     */
    @GetMapping("/v2/{id}")
    @Operation(summary = "查询规则详情（V2）", description = "根据规则ID查询详细信息，包含策略配置")
    public Result<RuleDetailVO> getRuleDetailV2(
        @Parameter(description = "规则ID") @PathVariable Long id
    ) {
        log.info("查询规则详情 V2: id={}", id);

        RuleService.RuleWithPolicy ruleWithPolicy = ruleService.getRuleWithPolicy(id);
        if (ruleWithPolicy == null || ruleWithPolicy.rule() == null) {
            return Result.error(ResultCode.RULE_NOT_FOUND);
        }

        RuleDetailVO vo = buildRuleDetailVO(ruleWithPolicy.rule(), ruleWithPolicy.policy());
        return Result.success(vo);
    }

    /**
     * 将 PolicyDTO 转换为实体
     */
    private RulePolicy convertPolicyDTOToEntity(RuleCreateDTO.PolicyDTO dto) {
        RulePolicy policy = new RulePolicy();
        policy.setScope(dto.getScope());
        policy.setWhitelist(dto.getWhitelist());
        policy.setBlacklist(dto.getBlacklist());
        policy.setRateLimitEnabled(dto.getRateLimitEnabled());
        policy.setRateLimitMaxRequests(dto.getRateLimitMaxRequests());
        policy.setRateLimitWindowSeconds(dto.getRateLimitWindowSeconds());
        policy.setTimeWindowEnabled(dto.getTimeWindowEnabled());
        policy.setTimeWindowStart(dto.getTimeWindowStart() != null ? java.time.LocalTime.parse(dto.getTimeWindowStart()) : null);
        policy.setTimeWindowEnd(dto.getTimeWindowEnd() != null ? java.time.LocalTime.parse(dto.getTimeWindowEnd()) : null);
        policy.setTimeWindowWeekdays(dto.getTimeWindowWeekdays());
        policy.setRoleEnabled(dto.getRoleEnabled());
        policy.setAllowedRoles(dto.getAllowedRoles());
        policy.setCooldownEnabled(dto.getCooldownEnabled());
        policy.setCooldownSeconds(dto.getCooldownSeconds());
        return policy;
    }

    /**
     * 构建 RuleDetailVO
     */
    private RuleDetailVO buildRuleDetailVO(MessageRule rule, RulePolicy policy) {
        // 转换 MatchType
        com.specqq.chatbot.common.enums.MatchType matchType = null;
        if (rule.getMatchType() != null) {
            matchType = com.specqq.chatbot.common.enums.MatchType.valueOf(rule.getMatchType().name());
        }

        RuleDetailVO.RuleDetailVOBuilder builder = RuleDetailVO.builder()
                .id(rule.getId())
                .ruleName(rule.getName())
                .description(rule.getDescription())
                .matchType(matchType)
                .pattern(rule.getPattern())
                .priority(rule.getPriority())
                .responseTemplate(rule.getResponseTemplate())
                .handlerConfig(rule.getHandlerConfig())
                .onErrorPolicy(rule.getOnErrorPolicy())
                .enabled(rule.getEnabled())
                .createBy(rule.getCreateBy())
                .createTime(rule.getCreateTime())
                .updateBy(rule.getUpdateBy())
                .updateTime(rule.getUpdateTime());

        // 添加策略信息
        if (policy != null) {
            RuleDetailVO.PolicyVO policyVO = RuleDetailVO.PolicyVO.builder()
                    .scope(policy.getScope())
                    .whitelist(policy.getWhitelist())
                    .blacklist(policy.getBlacklist())
                    .rateLimitEnabled(policy.getRateLimitEnabled())
                    .rateLimitMaxRequests(policy.getRateLimitMaxRequests())
                    .rateLimitWindowSeconds(policy.getRateLimitWindowSeconds())
                    .timeWindowEnabled(policy.getTimeWindowEnabled())
                    .timeWindowStart(policy.getTimeWindowStart() != null ? policy.getTimeWindowStart().toString() : null)
                    .timeWindowEnd(policy.getTimeWindowEnd() != null ? policy.getTimeWindowEnd().toString() : null)
                    .timeWindowWeekdays(policy.getTimeWindowWeekdays())
                    .roleEnabled(policy.getRoleEnabled())
                    .allowedRoles(policy.getAllowedRoles())
                    .cooldownEnabled(policy.getCooldownEnabled())
                    .cooldownSeconds(policy.getCooldownSeconds())
                    .build();
            builder.policy(policyVO);
        }

        return builder.build();
    }

    /**
     * 将 RuleUpdateDTO.PolicyDTO 转换为实体
     */
    private RulePolicy convertUpdatePolicyDTOToEntity(RuleUpdateDTO.PolicyDTO dto) {
        RulePolicy policy = new RulePolicy();
        policy.setScope(dto.getScope());
        policy.setWhitelist(dto.getWhitelist());
        policy.setBlacklist(dto.getBlacklist());
        policy.setRateLimitEnabled(dto.getRateLimitEnabled());
        policy.setRateLimitMaxRequests(dto.getRateLimitMaxRequests());
        policy.setRateLimitWindowSeconds(dto.getRateLimitWindowSeconds());
        policy.setTimeWindowEnabled(dto.getTimeWindowEnabled());
        policy.setTimeWindowStart(dto.getTimeWindowStart() != null ? java.time.LocalTime.parse(dto.getTimeWindowStart()) : null);
        policy.setTimeWindowEnd(dto.getTimeWindowEnd() != null ? java.time.LocalTime.parse(dto.getTimeWindowEnd()) : null);
        policy.setTimeWindowWeekdays(dto.getTimeWindowWeekdays());
        policy.setRoleEnabled(dto.getRoleEnabled());
        policy.setAllowedRoles(dto.getAllowedRoles());
        policy.setCooldownEnabled(dto.getCooldownEnabled());
        policy.setCooldownSeconds(dto.getCooldownSeconds());
        return policy;
    }

    /**
     * 转换 MatchType 从 DTO 到 Entity
     */
    private MessageRule.MatchType convertToEntityMatchType(com.specqq.chatbot.common.enums.MatchType dtoMatchType) {
        if (dtoMatchType == null) {
            return null;
        }
        return MessageRule.MatchType.valueOf(dtoMatchType.name());
    }

    /**
     * Test rule matching
     *
     * <p>T073: Test if a message matches a rule and passes policy checks</p>
     */
    @PostMapping("/test")
    @Operation(summary = "Test rule matching", description = "Test if a message matches a rule and passes policy checks")
    public Result<TestRuleResult> testRule(@Valid @RequestBody TestRuleRequest request) {
        log.info("Testing rule: ruleId={}, message={}", request.getRuleId(), request.getMessage());

        try {
            // Get rule
            MessageRule rule = ruleService.getRuleById(request.getRuleId());
            if (rule == null) {
                return Result.error(ResultCode.NOT_FOUND, "Rule not found");
            }

            // Create test message DTO
            com.specqq.chatbot.dto.MessageReceiveDTO testMessage = com.specqq.chatbot.dto.MessageReceiveDTO.builder()
                    .messageId("test-" + System.currentTimeMillis())
                    .groupId(request.getGroupId() != null ? request.getGroupId() : "test-group")
                    .userId(request.getUserId() != null ? request.getUserId() : "test-user")
                    .messageContent(request.getMessage())
                    .timestamp(java.time.LocalDateTime.now())
                    .build();

            // Test rule matching
            com.specqq.chatbot.engine.RuleMatcher matcher = getMatcherForType(rule.getMatchType());
            boolean matched = matcher != null && matcher.matches(request.getMessage(), rule.getPattern());

            TestRuleResult result = new TestRuleResult();
            result.setMatched(matched);
            result.setRuleName(rule.getName());
            result.setMatchType(rule.getMatchType().name());
            result.setPattern(rule.getPattern());

            if (!matched) {
                result.setPolicyPassed(false);
                result.setFailedPolicy(null);
                result.setReason("Message does not match pattern");
                return Result.success(result);
            }

            // Test policy checks if rule matched
            RulePolicy policy = policyService.getPolicyByRuleId(rule.getId());
            if (policy != null) {
                com.specqq.chatbot.engine.policy.PolicyChain policyChain = getPolicyChain();
                com.specqq.chatbot.engine.policy.PolicyChain.PolicyCheckResult policyResult =
                        policyChain.check(testMessage, policy);

                result.setPolicyPassed(policyResult.isPassed());
                result.setFailedPolicy(policyResult.getFailedPolicy());
                result.setReason(policyResult.getReason());
            } else {
                result.setPolicyPassed(true);
                result.setFailedPolicy(null);
                result.setReason("No policy configured");
            }

            return Result.success(result);

        } catch (Exception e) {
            log.error("Failed to test rule: ruleId={}", request.getRuleId(), e);
            return Result.error(ResultCode.INTERNAL_ERROR, "Failed to test rule: " + e.getMessage());
        }
    }

    /**
     * Get matcher for match type
     */
    private com.specqq.chatbot.engine.RuleMatcher getMatcherForType(MessageRule.MatchType matchType) {
        // This is a simplified implementation for testing
        // In production, inject matchers via constructor
        try {
            return switch (matchType) {
                case EXACT -> new com.specqq.chatbot.engine.ExactMatcher();
                case CONTAINS -> new com.specqq.chatbot.engine.ContainsMatcher();
                case REGEX -> new com.specqq.chatbot.engine.RegexMatcher(
                        com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                                .maximumSize(1000)
                                .build()
                );
                case PREFIX -> new com.specqq.chatbot.engine.PrefixMatcher();
                case SUFFIX -> new com.specqq.chatbot.engine.SuffixMatcher();
                default -> null;
            };
        } catch (Exception e) {
            log.error("Failed to create matcher for type: {}", matchType, e);
            return null;
        }
    }

    /**
     * Get policy chain (simplified for testing)
     *
     * <p>Note: This creates a minimal policy chain for testing purposes only</p>
     * <p>Some interceptors (RateLimit, Cooldown) are disabled to avoid Redis dependency</p>
     */
    private com.specqq.chatbot.engine.policy.PolicyChain getPolicyChain() {
        // Create a test-only policy chain with minimal dependencies
        // Only Scope and TimeWindow interceptors will work properly in test mode
        return new com.specqq.chatbot.engine.policy.PolicyChain() {
            @Override
            public PolicyCheckResult check(com.specqq.chatbot.dto.MessageReceiveDTO message,
                                          com.specqq.chatbot.entity.RulePolicy policy) {
                if (policy == null) {
                    return PolicyCheckResult.pass();
                }

                // Only check scope and time window for testing
                com.specqq.chatbot.interceptor.ScopeInterceptor scopeInterceptor =
                        new com.specqq.chatbot.interceptor.ScopeInterceptor();
                if (!scopeInterceptor.intercept(message, policy)) {
                    return PolicyCheckResult.fail("Scope", scopeInterceptor.getInterceptReason());
                }

                com.specqq.chatbot.interceptor.TimeWindowInterceptor timeWindowInterceptor =
                        new com.specqq.chatbot.interceptor.TimeWindowInterceptor();
                if (!timeWindowInterceptor.intercept(message, policy)) {
                    return PolicyCheckResult.fail("TimeWindow", timeWindowInterceptor.getInterceptReason());
                }

                // Skip RateLimit, Role, and Cooldown checks in test mode
                return PolicyCheckResult.pass();
            }
        };
    }

    /**
     * Test rule request DTO
     */
    public static class TestRuleRequest {
        private Long ruleId;
        private String message;
        private String groupId;
        private String userId;

        // Legacy fields for direct pattern testing (without rule ID)
        private MessageRule.MatchType matchType;
        private String matchPattern;  // Legacy field name
        private String pattern;        // New field name (preferred)
        private String testMessage;

        public Long getRuleId() {
            return ruleId;
        }

        public void setRuleId(Long ruleId) {
            this.ruleId = ruleId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public MessageRule.MatchType getMatchType() {
            return matchType;
        }

        public void setMatchType(MessageRule.MatchType matchType) {
            this.matchType = matchType;
        }

        /**
         * Get pattern (supports both 'pattern' and 'matchPattern' field names)
         */
        public String getMatchPattern() {
            // Prefer 'pattern' if set, fallback to 'matchPattern'
            return pattern != null ? pattern : matchPattern;
        }

        public void setMatchPattern(String matchPattern) {
            this.matchPattern = matchPattern;
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public String getTestMessage() {
            return testMessage;
        }

        public void setTestMessage(String testMessage) {
            this.testMessage = testMessage;
        }
    }

    /**
     * Test rule result DTO
     */
    public static class TestRuleResult {
        private boolean matched;
        private String message;
        private String ruleName;
        private String matchType;
        private String pattern;
        private boolean policyPassed;
        private String failedPolicy;
        private String reason;

        public boolean isMatched() {
            return matched;
        }

        public void setMatched(boolean matched) {
            this.matched = matched;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getRuleName() {
            return ruleName;
        }

        public void setRuleName(String ruleName) {
            this.ruleName = ruleName;
        }

        public String getMatchType() {
            return matchType;
        }

        public void setMatchType(String matchType) {
            this.matchType = matchType;
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public boolean isPolicyPassed() {
            return policyPassed;
        }

        public void setPolicyPassed(boolean policyPassed) {
            this.policyPassed = policyPassed;
        }

        public String getFailedPolicy() {
            return failedPolicy;
        }

        public void setFailedPolicy(String failedPolicy) {
            this.failedPolicy = failedPolicy;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
