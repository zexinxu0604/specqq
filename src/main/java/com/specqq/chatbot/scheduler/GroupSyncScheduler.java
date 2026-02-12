package com.specqq.chatbot.scheduler;

import com.specqq.chatbot.dto.BatchSyncResultDTO;
import com.specqq.chatbot.entity.GroupChat;
import com.specqq.chatbot.service.DefaultRuleService;
import com.specqq.chatbot.service.GroupSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 群组同步定时任务
 * 定期同步群组信息，确保数据准确性
 *
 * @author Claude Code
 * @since 2026-02-12
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupSyncScheduler {

    private final GroupSyncService groupSyncService;
    private final DefaultRuleService defaultRuleService;

    /**
     * 定时同步所有活跃群组
     * 默认每6小时执行一次
     */
    @Scheduled(cron = "${sync.task.cron:0 0 */6 * * ?}")
    public void syncAllActiveGroups() {
        log.info("=== 开始定时同步所有活跃群组 ===");

        try {
            BatchSyncResultDTO result = groupSyncService.syncAllActiveGroups();

            log.info("定时同步完成: total={}, success={}, failure={}, successRate={}%, duration={}ms",
                    result.totalCount(),
                    result.successCount(),
                    result.failureCount(),
                    String.format("%.2f", result.getSuccessRate()),
                    result.durationMs());

            // 检查是否有需要告警的失败群组
            if (result.failureCount() > 0) {
                List<GroupChat> alertGroups = groupSyncService.getAlertGroups();
                if (!alertGroups.isEmpty()) {
                    log.warn("发现需要告警的失败群组: count={}", alertGroups.size());
                    alertGroups.forEach(group ->
                            log.warn("告警群组: groupId={}, groupName={}, consecutiveFailures={}, reason={}",
                                    group.getGroupId(),
                                    group.getGroupName(),
                                    group.getConsecutiveFailureCount(),
                                    group.getFailureReason())
                    );
                }
            }

        } catch (Exception e) {
            log.error("定时同步任务执行异常", e);
        }

        log.info("=== 定时同步任务结束 ===");
    }

    /**
     * 定时重试失败的群组
     * 默认每小时执行一次 (cron: 0 0 * * * ?)
     * 重试连续失败次数 >= 1 的群组
     */
    @Scheduled(cron = "${sync.retry.cron:0 0 * * * ?}")
    public void retryFailedGroups() {
        log.info("=== 开始定时重试失败群组 ===");

        try {
            int minFailureCount = 1; // 重试所有失败的群组
            BatchSyncResultDTO result = groupSyncService.retryFailedGroups(minFailureCount);

            if (result.totalCount() == 0) {
                log.info("没有需要重试的失败群组");
                return;
            }

            log.info("重试失败群组完成: total={}, success={}, failure={}, successRate={}%, duration={}ms",
                    result.totalCount(),
                    result.successCount(),
                    result.failureCount(),
                    String.format("%.2f", result.getSuccessRate()),
                    result.durationMs());

            // 记录仍然失败的群组
            if (result.failureCount() > 0) {
                log.warn("重试后仍然失败的群组数量: {}", result.failureCount());
            }

        } catch (Exception e) {
            log.error("重试失败群组任务执行异常", e);
        }

        log.info("=== 重试失败群组任务结束 ===");
    }

    /**
     * 手动触发同步所有活跃群组
     * 用于测试或手动触发
     */
    public BatchSyncResultDTO triggerManualSync() {
        log.info("手动触发同步所有活跃群组");
        return groupSyncService.syncAllActiveGroups();
    }

    /**
     * 手动触发重试失败群组
     * 用于测试或手动触发
     *
     * @param minFailureCount 最小失败次数阈值
     */
    public BatchSyncResultDTO triggerManualRetry(Integer minFailureCount) {
        log.info("手动触发重试失败群组: minFailureCount={}", minFailureCount);
        return groupSyncService.retryFailedGroups(minFailureCount);
    }

    /**
     * 定时同步默认规则
     * 为未绑定默认规则的群组自动绑定规则
     * 默认每天执行一次 (cron: 0 0 2 * * ?)
     */
    @Scheduled(cron = "${sync.default-rules.cron:0 0 2 * * ?}")
    public void syncDefaultRules() {
        log.info("=== 开始定时同步默认规则 ===");

        try {
            // 查询未绑定默认规则的群组
            List<GroupChat> groupsWithoutRules = defaultRuleService.getGroupsWithoutDefaultRules();

            if (groupsWithoutRules.isEmpty()) {
                log.info("所有群组已绑定默认规则，无需同步");
                return;
            }

            log.info("发现 {} 个群组未绑定默认规则", groupsWithoutRules.size());

            // 批量绑定默认规则
            Integer totalBindCount = defaultRuleService.batchApplyDefaultRules(groupsWithoutRules);

            log.info("默认规则同步完成: groups={}, totalBindCount={}",
                    groupsWithoutRules.size(), totalBindCount);

        } catch (Exception e) {
            log.error("定时同步默认规则任务执行异常", e);
        }

        log.info("=== 定时同步默认规则任务结束 ===");
    }

    /**
     * 手动触发默认规则同步
     * 用于测试或手动触发
     *
     * @return 绑定的规则总数
     */
    public Integer triggerManualDefaultRuleSync() {
        log.info("手动触发默认规则同步");

        List<GroupChat> groupsWithoutRules = defaultRuleService.getGroupsWithoutDefaultRules();

        if (groupsWithoutRules.isEmpty()) {
            log.info("所有群组已绑定默认规则");
            return 0;
        }

        Integer totalBindCount = defaultRuleService.batchApplyDefaultRules(groupsWithoutRules);
        log.info("手动同步完成: groups={}, totalBindCount={}",
                groupsWithoutRules.size(), totalBindCount);

        return totalBindCount;
    }
}
