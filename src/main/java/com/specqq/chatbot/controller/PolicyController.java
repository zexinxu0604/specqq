package com.specqq.chatbot.controller;

import com.specqq.chatbot.common.Result;
import com.specqq.chatbot.common.ResultCode;
import com.specqq.chatbot.dto.PolicyDTO;
import com.specqq.chatbot.entity.RulePolicy;
import com.specqq.chatbot.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 策略管理控制器
 *
 * <p>提供策略配置的增删改查接口</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Slf4j
@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
@Validated
@Tag(name = "策略管理", description = "规则策略的增删改查接口")
public class PolicyController {

    private final PolicyService policyService;

    /**
     * 根据规则 ID 查询策略
     */
    @GetMapping("/rule/{ruleId}")
    @Operation(summary = "查询规则策略", description = "根据规则ID查询策略配置")
    public Result<PolicyDTO> getPolicyByRuleId(
        @Parameter(description = "规则ID") @PathVariable Long ruleId
    ) {
        log.info("查询规则策略: ruleId={}", ruleId);

        RulePolicy policy = policyService.getPolicyByRuleId(ruleId);
        if (policy == null) {
            return Result.error(ResultCode.NOT_FOUND, "策略不存在");
        }

        PolicyDTO dto = convertEntityToDTO(policy);
        return Result.success(dto);
    }

    /**
     * 创建策略
     */
    @PostMapping
    @Operation(summary = "创建策略", description = "为规则创建策略配置")
    public Result<PolicyDTO> createPolicy(
        @Valid @RequestBody PolicyDTO dto,
        @Parameter(description = "规则ID") @RequestParam Long ruleId
    ) {
        log.info("创建策略: ruleId={}", ruleId);

        // 检查规则是否已有策略
        RulePolicy existing = policyService.getPolicyByRuleId(ruleId);
        if (existing != null) {
            return Result.error(ResultCode.BAD_REQUEST, "规则已存在策略配置");
        }

        // 构建策略实体
        RulePolicy policy = convertDTOToEntity(dto);
        policy.setRuleId(ruleId);

        // 验证策略配置
        String validationError = policyService.validatePolicy(policy);
        if (validationError != null) {
            return Result.error(ResultCode.BAD_REQUEST, validationError);
        }

        // 创建策略
        RulePolicy created = policyService.createPolicy(policy);
        PolicyDTO result = convertEntityToDTO(created);

        return Result.success("策略创建成功", result);
    }

    /**
     * 更新策略
     */
    @PutMapping("/rule/{ruleId}")
    @Operation(summary = "更新策略", description = "更新规则的策略配置")
    public Result<PolicyDTO> updatePolicy(
        @Parameter(description = "规则ID") @PathVariable Long ruleId,
        @Valid @RequestBody PolicyDTO dto
    ) {
        log.info("更新策略: ruleId={}", ruleId);

        // 检查策略是否存在
        RulePolicy existing = policyService.getPolicyByRuleId(ruleId);
        if (existing == null) {
            return Result.error(ResultCode.NOT_FOUND, "策略不存在");
        }

        // 更新策略字段
        updateEntityFromDTO(existing, dto);

        // 验证策略配置
        String validationError = policyService.validatePolicy(existing);
        if (validationError != null) {
            return Result.error(ResultCode.BAD_REQUEST, validationError);
        }

        // 更新策略
        RulePolicy updated = policyService.updatePolicy(existing);
        PolicyDTO result = convertEntityToDTO(updated);

        return Result.success("策略更新成功", result);
    }

    /**
     * 删除策略
     */
    @DeleteMapping("/rule/{ruleId}")
    @Operation(summary = "删除策略", description = "删除规则的策略配置")
    public Result<Void> deletePolicy(
        @Parameter(description = "规则ID") @PathVariable Long ruleId
    ) {
        log.info("删除策略: ruleId={}", ruleId);

        // 检查策略是否存在
        RulePolicy existing = policyService.getPolicyByRuleId(ruleId);
        if (existing == null) {
            return Result.error(ResultCode.NOT_FOUND, "策略不存在");
        }

        // 删除策略
        policyService.deletePolicyByRuleId(ruleId);

        return Result.success("策略删除成功", null);
    }

    /**
     * 验证策略配置
     */
    @PostMapping("/validate")
    @Operation(summary = "验证策略配置", description = "验证策略配置是否合法")
    public Result<Boolean> validatePolicy(
        @Valid @RequestBody PolicyDTO dto
    ) {
        log.info("验证策略配置");

        // 构建策略实体
        RulePolicy policy = convertDTOToEntity(dto);

        // 验证策略配置
        String validationError = policyService.validatePolicy(policy);
        if (validationError != null) {
            return Result.error(ResultCode.BAD_REQUEST, validationError);
        }

        return Result.success("策略配置有效", true);
    }

    /**
     * 将实体转换为 DTO
     */
    private PolicyDTO convertEntityToDTO(RulePolicy policy) {
        return PolicyDTO.builder()
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
    }

    /**
     * 将 DTO 转换为实体
     */
    private RulePolicy convertDTOToEntity(PolicyDTO dto) {
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
     * 从 DTO 更新实体
     */
    private void updateEntityFromDTO(RulePolicy policy, PolicyDTO dto) {
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
    }
}
