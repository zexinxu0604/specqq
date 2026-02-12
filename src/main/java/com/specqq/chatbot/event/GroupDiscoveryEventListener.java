package com.specqq.chatbot.event;

import com.specqq.chatbot.entity.GroupChat;
import com.specqq.chatbot.service.DefaultRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 群组发现事件监听器
 * 监听新群组发现事件，自动为新群组绑定默认规则
 *
 * @author Claude Code
 * @since 2026-02-12
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupDiscoveryEventListener {

    private final DefaultRuleService defaultRuleService;

    /**
     * 处理群组发现事件
     * 异步执行，避免阻塞主流程
     *
     * @param event 群组发现事件
     */
    @Async
    @EventListener
    public void handleGroupDiscoveryEvent(GroupDiscoveryEvent event) {
        List<GroupChat> newGroups = event.getNewGroups();
        Long clientId = event.getClientId();

        if (newGroups == null || newGroups.isEmpty()) {
            log.debug("群组发现事件：没有新群组需要处理，clientId={}", clientId);
            return;
        }

        log.info("=== 处理群组发现事件 ===");
        log.info("客户端ID: {}", clientId);
        log.info("新群组数量: {}", newGroups.size());
        log.info("新群组列表: {}", newGroups.stream()
                .map(g -> String.format("%s(%s)", g.getGroupName(), g.getGroupId()))
                .toList());

        try {
            // 为新群组批量绑定默认规则
            Integer totalBindCount = defaultRuleService.batchApplyDefaultRules(newGroups);

            log.info("默认规则绑定完成: 为 {} 个群组绑定了 {} 条规则",
                    newGroups.size(), totalBindCount);

            // 记录每个群组的绑定结果
            for (GroupChat group : newGroups) {
                Boolean hasDefaultRules = defaultRuleService.hasDefaultRules(group.getId());
                log.debug("群组 {}({}) 默认规则绑定状态: {}",
                        group.getGroupName(),
                        group.getGroupId(),
                        hasDefaultRules ? "已绑定" : "未绑定");
            }

        } catch (Exception e) {
            log.error("处理群组发现事件失败: clientId={}, newGroupCount={}",
                    clientId, newGroups.size(), e);
        }

        log.info("=== 群组发现事件处理完成 ===");
    }
}
