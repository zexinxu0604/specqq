package com.specqq.chatbot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.specqq.chatbot.common.Result;
import com.specqq.chatbot.common.ResultCode;
import com.specqq.chatbot.entity.GroupChat;
import com.specqq.chatbot.entity.GroupRuleConfig;
import com.specqq.chatbot.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 群聊管理控制器
 *
 * @author Chatbot Router System
 */
@Slf4j
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Validated
@Tag(name = "群聊管理", description = "群聊和规则配置管理接口")
public class GroupController {

    private final GroupService groupService;

    /**
     * 分页查询群聊列表
     */
    @GetMapping
    @Operation(summary = "分页查询群聊", description = "支持按群名称、群ID、启用状态筛选")
    public Result<Page<GroupChat>> listGroups(
        @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") Integer page,
        @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer size,
        @Parameter(description = "群名称或群ID（模糊查询）") @RequestParam(required = false) String keyword,
        @Parameter(description = "客户端ID") @RequestParam(required = false) Long clientId,
        @Parameter(description = "启用状态") @RequestParam(required = false) Boolean enabled
    ) {
        log.info("查询群聊列表: page={}, size={}, keyword={}, clientId={}, enabled={}",
            page, size, keyword, clientId, enabled);

        Page<GroupChat> result = groupService.listGroups(page, size, keyword, clientId, enabled);
        return Result.success(result);
    }

    /**
     * 根据ID查询群聊详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询群聊详情", description = "根据群聊ID查询详细信息")
    public Result<GroupChat> getGroupById(
        @Parameter(description = "群聊ID") @PathVariable Long id
    ) {
        log.info("查询群聊详情: id={}", id);

        GroupChat group = groupService.getGroupById(id);
        if (group == null) {
            return Result.error(ResultCode.GROUP_NOT_FOUND);
        }

        return Result.success(group);
    }

    /**
     * 更新群聊配置
     */
    @PutMapping("/{id}/config")
    @Operation(summary = "更新群聊配置", description = "更新群聊的配置信息（如消息频率限制）")
    public Result<GroupChat> updateGroupConfig(
        @Parameter(description = "群聊ID") @PathVariable Long id,
        @Valid @RequestBody GroupChat.GroupConfig config
    ) {
        log.info("更新群聊配置: id={}, config={}", id, config);

        GroupChat group = groupService.getGroupById(id);
        if (group == null) {
            return Result.error(ResultCode.GROUP_NOT_FOUND);
        }

        groupService.updateGroupConfig(id, config);
        GroupChat updated = groupService.getGroupById(id);
        return Result.success("群聊配置更新成功", updated);
    }

    /**
     * 切换群聊启用状态
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "切换群聊状态", description = "启用或禁用群聊")
    public Result<Void> toggleGroupStatus(
        @Parameter(description = "群聊ID") @PathVariable Long id,
        @Parameter(description = "启用状态") @RequestParam Boolean enabled
    ) {
        log.info("切换群聊状态: id={}, enabled={}", id, enabled);

        GroupChat group = groupService.getGroupById(id);
        if (group == null) {
            return Result.error(ResultCode.GROUP_NOT_FOUND);
        }

        groupService.toggleGroupStatus(id, enabled);
        return Result.success(enabled ? "群聊已启用" : "群聊已禁用", null);
    }

    /**
     * 查询群聊的规则配置
     */
    @GetMapping("/{id}/rules")
    @Operation(summary = "查询群聊规则", description = "查询群聊已启用的规则列表")
    public Result<List<GroupRuleConfig>> getGroupRules(
        @Parameter(description = "群聊ID") @PathVariable Long id
    ) {
        log.info("查询群聊规则: groupId={}", id);

        GroupChat group = groupService.getGroupById(id);
        if (group == null) {
            return Result.error(ResultCode.GROUP_NOT_FOUND);
        }

        List<GroupRuleConfig> rules = groupService.getGroupRuleConfigs(id);
        return Result.success(rules);
    }

    /**
     * 为群聊批量启用规则
     */
    @PostMapping("/{id}/rules")
    @Operation(summary = "批量启用规则", description = "为群聊批量启用或禁用规则")
    public Result<Void> batchEnableRules(
        @Parameter(description = "群聊ID") @PathVariable Long id,
        @Parameter(description = "规则ID列表") @RequestBody List<Long> ruleIds,
        @Parameter(description = "启用状态") @RequestParam(defaultValue = "true") Boolean enabled
    ) {
        log.info("批量启用规则: groupId={}, ruleIds={}, enabled={}", id, ruleIds, enabled);

        GroupChat group = groupService.getGroupById(id);
        if (group == null) {
            return Result.error(ResultCode.GROUP_NOT_FOUND);
        }

        if (ruleIds == null || ruleIds.isEmpty()) {
            return Result.error(ResultCode.BAD_REQUEST, "规则ID列表不能为空");
        }

        int count = groupService.batchEnableRules(id, ruleIds, enabled);
        return Result.success(String.format("成功%s %d 条规则", enabled ? "启用" : "禁用", count), null);
    }

    /**
     * 为群聊添加单个规则
     */
    @PostMapping("/{id}/rules/{ruleId}")
    @Operation(summary = "添加规则", description = "为群聊添加单个规则")
    public Result<GroupRuleConfig> addRuleToGroup(
        @Parameter(description = "群聊ID") @PathVariable Long id,
        @Parameter(description = "规则ID") @PathVariable Long ruleId
    ) {
        log.info("为群聊添加规则: groupId={}, ruleId={}", id, ruleId);

        GroupChat group = groupService.getGroupById(id);
        if (group == null) {
            return Result.error(ResultCode.GROUP_NOT_FOUND);
        }

        GroupRuleConfig config = groupService.addRuleToGroup(id, ruleId);
        return Result.success("规则添加成功", config);
    }

    /**
     * 从群聊移除规则
     */
    @DeleteMapping("/{id}/rules/{ruleId}")
    @Operation(summary = "移除规则", description = "从群聊移除指定规则")
    public Result<Void> removeRuleFromGroup(
        @Parameter(description = "群聊ID") @PathVariable Long id,
        @Parameter(description = "规则ID") @PathVariable Long ruleId
    ) {
        log.info("从群聊移除规则: groupId={}, ruleId={}", id, ruleId);

        groupService.removeRuleFromGroup(id, ruleId);
        return Result.success("规则移除成功", null);
    }

    /**
     * 切换群聊规则启用状态
     */
    @PatchMapping("/{id}/rules/{ruleId}/status")
    @Operation(summary = "切换规则状态", description = "切换群聊中指定规则的启用状态")
    public Result<Void> toggleGroupRuleStatus(
        @Parameter(description = "群聊ID") @PathVariable Long id,
        @Parameter(description = "规则ID") @PathVariable Long ruleId,
        @Parameter(description = "启用状态") @RequestParam Boolean enabled
    ) {
        log.info("切换群聊规则状态: groupId={}, ruleId={}, enabled={}", id, ruleId, enabled);

        groupService.toggleGroupRuleStatus(id, ruleId, enabled);
        return Result.success(enabled ? "规则已启用" : "规则已禁用", null);
    }

    /**
     * 查询群聊统计信息
     */
    @GetMapping("/{id}/stats")
    @Operation(summary = "查询群聊统计", description = "查询群聊的消息统计信息")
    public Result<java.util.Map<String, Object>> getGroupStats(
        @Parameter(description = "群聊ID") @PathVariable Long id,
        @Parameter(description = "开始时间（ISO 8601格式）") @RequestParam(required = false) String startTime,
        @Parameter(description = "结束时间（ISO 8601格式）") @RequestParam(required = false) String endTime
    ) {
        log.info("查询群聊统计: groupId={}, startTime={}, endTime={}", id, startTime, endTime);

        GroupChat group = groupService.getGroupById(id);
        if (group == null) {
            return Result.error(ResultCode.GROUP_NOT_FOUND);
        }

        java.util.Map<String, Object> stats = groupService.getGroupStats(id, startTime, endTime);
        return Result.success(stats);
    }

    /**
     * 同步群聊信息
     */
    @PostMapping("/{id}/sync")
    @Operation(summary = "同步群聊信息", description = "从客户端同步群聊的最新信息（群名称、成员数等）")
    public Result<GroupChat> syncGroupInfo(
        @Parameter(description = "群聊ID") @PathVariable Long id
    ) {
        log.info("同步群聊信息: id={}", id);

        GroupChat group = groupService.getGroupById(id);
        if (group == null) {
            return Result.error(ResultCode.GROUP_NOT_FOUND);
        }

        GroupChat synced = groupService.syncGroupInfo(id);
        return Result.success("群聊信息同步成功", synced);
    }

    /**
     * 批量导入群聊
     */
    @PostMapping("/batch-import")
    @Operation(summary = "批量导入群聊", description = "从客户端批量导入群聊")
    public Result<java.util.Map<String, Object>> batchImportGroups(
        @Parameter(description = "客户端ID") @RequestParam Long clientId
    ) {
        log.info("批量导入群聊: clientId={}", clientId);

        java.util.Map<String, Object> result = groupService.batchImportGroups(clientId);
        return Result.success("群聊导入完成", result);
    }
}
