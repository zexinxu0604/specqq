package com.specqq.chatbot.controller;

import com.specqq.chatbot.common.Result;
import com.specqq.chatbot.dto.BatchSyncResultDTO;
import com.specqq.chatbot.entity.GroupChat;
import com.specqq.chatbot.scheduler.GroupSyncScheduler;
import com.specqq.chatbot.service.GroupSyncService;
import com.specqq.chatbot.vo.BatchSyncVO;
import com.specqq.chatbot.vo.GroupSyncVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 群组同步控制器
 * 提供群组信息同步的手动触发接口
 *
 * @author Claude Code
 * @since 2026-02-12
 */
@Slf4j
@RestController
@RequestMapping("/api/groups/sync")
@RequiredArgsConstructor
@Tag(name = "群组同步管理", description = "群组信息同步相关接口")
public class GroupSyncController {

    private final GroupSyncService groupSyncService;
    private final GroupSyncScheduler groupSyncScheduler;

    @PostMapping("/trigger")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "手动触发同步所有活跃群组", description = "立即执行一次全量同步任务")
    public Result<BatchSyncVO> triggerSync() {
        log.info("手动触发同步所有活跃群组");
        BatchSyncResultDTO result = groupSyncScheduler.triggerManualSync();
        return Result.success(BatchSyncVO.from(result));
    }

    @PostMapping("/retry")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "重试失败的群组同步", description = "对连续失败次数达到阈值的群组进行重试")
    public Result<BatchSyncVO> retryFailedGroups(
            @Parameter(description = "最小失败次数阈值，默认1")
            @RequestParam(defaultValue = "1") Integer minFailureCount) {
        log.info("手动触发重试失败群组: minFailureCount={}", minFailureCount);
        BatchSyncResultDTO result = groupSyncScheduler.triggerManualRetry(minFailureCount);
        return Result.success(BatchSyncVO.from(result));
    }

    @PostMapping("/{groupId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "同步单个群组", description = "立即同步指定群组的信息")
    public Result<GroupSyncVO> syncGroup(
            @Parameter(description = "群组ID")
            @PathVariable Long groupId) {
        log.info("手动同步单个群组: groupId={}", groupId);
        // TODO: 需要先查询群组实体
        return Result.error("功能开发中");
    }

    @GetMapping("/alert")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取需要告警的失败群组", description = "查询连续失败次数 >= 3 的群组列表")
    public Result<List<GroupSyncVO>> getAlertGroups() {
        log.info("查询需要告警的失败群组");
        List<GroupChat> alertGroups = groupSyncService.getAlertGroups();
        List<GroupSyncVO> vos = alertGroups.stream()
                .map(GroupSyncVO::from)
                .collect(Collectors.toList());
        return Result.success(vos);
    }

    @PostMapping("/{groupId}/reset")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "重置群组失败计数", description = "手动重置指定群组的失败计数和失败原因")
    public Result<Void> resetFailureCount(
            @Parameter(description = "群组ID")
            @PathVariable Long groupId) {
        log.info("重置群组失败计数: groupId={}", groupId);
        groupSyncService.resetFailureCount(groupId);
        return Result.success();
    }

    @PostMapping("/discover/{clientId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "自动发现新群组", description = "从NapCat获取机器人所在的所有群组，添加不存在的群组")
    public Result<Integer> discoverNewGroups(
            @Parameter(description = "客户端ID")
            @PathVariable Long clientId) {
        log.info("自动发现新群组: clientId={}", clientId);
        Integer newGroupCount = groupSyncService.discoverNewGroups(clientId);
        return Result.success("成功发现并添加 " + newGroupCount + " 个新群组", newGroupCount);
    }
}
