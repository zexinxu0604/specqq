package com.specqq.chatbot.service.impl;

import com.specqq.chatbot.adapter.ClientAdapter;
import com.specqq.chatbot.adapter.ClientAdapterFactory;
import com.specqq.chatbot.adapter.NapCatAdapter;
import com.specqq.chatbot.constant.SyncConstants;
import com.specqq.chatbot.dto.ApiCallResponseDTO;
import com.specqq.chatbot.dto.BatchSyncResultDTO;
import com.specqq.chatbot.dto.GroupSyncResultDTO;
import com.specqq.chatbot.entity.ChatClient;
import com.specqq.chatbot.entity.GroupChat;
import com.specqq.chatbot.enums.SyncStatus;
import com.specqq.chatbot.mapper.ChatClientMapper;
import com.specqq.chatbot.mapper.GroupChatMapper;
import com.specqq.chatbot.event.GroupDiscoveryEvent;
import com.specqq.chatbot.service.GroupSyncService;
import com.specqq.chatbot.service.MetricsService;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 群组同步服务实现
 *
 * @author Claude Code
 * @since 2026-02-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupSyncServiceImpl implements GroupSyncService {

    private final GroupChatMapper groupChatMapper;
    private final ChatClientMapper chatClientMapper;
    private final ClientAdapterFactory clientAdapterFactory;
    private final ApplicationEventPublisher eventPublisher;
    private final MetricsService metricsService;

    @Override
    @Retry(name = "groupSync", fallbackMethod = "syncGroupFallback")
    public GroupSyncResultDTO syncGroup(GroupChat groupChat) {
        log.info("开始同步群组: groupId={}, groupName={}", groupChat.getGroupId(), groupChat.getGroupName());

        try {
            // 获取客户端适配器
            ChatClient client = groupChat.getClient();
            if (client == null) {
                log.debug("客户端信息未加载，从数据库查询: clientId={}", groupChat.getClientId());
                client = chatClientMapper.selectById(groupChat.getClientId());
                groupChat.setClient(client);
            }

            log.debug("获取客户端适配器: clientType={}", client.getClientType());
            ClientAdapter adapter = clientAdapterFactory.getAdapter(client.getClientType());
            if (!(adapter instanceof NapCatAdapter napCatAdapter)) {
                throw new IllegalStateException("Only NapCat adapter is supported for sync");
            }

            // 调用 NapCat API 获取群组信息
            Long groupIdLong = Long.parseLong(groupChat.getGroupId());
            log.debug("调用NapCat API获取群组信息: groupId={}", groupIdLong);
            CompletableFuture<ApiCallResponseDTO> future = napCatAdapter.getGroupInfo(groupIdLong);
            ApiCallResponseDTO response = future.join();
            log.debug("NapCat API响应: retcode={}, hasData={}",
                response != null ? response.getRetcode() : null,
                response != null && response.getData() != null);

            if (response == null || response.getRetcode() != 0) {
                // API 调用失败或机器人不在群组中
                String reason = response != null ? "API返回错误: retcode=" + response.getRetcode() : "机器人已不在群组中";
                groupChat.markSyncFailure(reason);
                groupChat.setActive(false);
                groupChatMapper.updateById(groupChat);

                log.warn("群组同步失败: groupId={}, reason={}", groupChat.getGroupId(), reason);

                // Record failure metric
                metricsService.recordGroupSyncFailure(groupChat.getGroupId(),
                    response == null ? "bot_removed" : "api_error");

                return GroupSyncResultDTO.failure(groupChat.getId(), groupChat.getGroupName(), reason, false);
            }

            // 解析群组信息
            Map<String, Object> data = response.getData();
            String groupName = (String) data.get("group_name");
            Integer memberCount = getIntValue(data, "member_count");
            log.debug("解析群组信息: groupName={}, memberCount={}", groupName, memberCount);

            // 更新群组信息
            if (groupName != null) {
                groupChat.setGroupName(groupName);
            }
            if (memberCount != null) {
                groupChat.setMemberCount(memberCount);
            }

            // 标记同步成功
            groupChat.markSyncSuccess();
            log.debug("标记同步成功: groupId={}, lastSyncTime={}", groupChat.getGroupId(), groupChat.getLastSyncTime());
            groupChat.setActive(true);
            groupChatMapper.updateById(groupChat);

            log.info("群组同步成功: groupId={}, groupName={}, memberCount={}",
                    groupChat.getGroupId(), groupName, memberCount);

            // Record success metric
            metricsService.recordGroupSyncSuccess(groupChat.getGroupId());

            return GroupSyncResultDTO.success(groupChat.getId(), groupChat.getGroupName(), memberCount);

        } catch (Exception e) {
            log.error("群组同步异常: groupId={}, groupName={}",
                    groupChat.getGroupId(), groupChat.getGroupName(), e);
            groupChat.markSyncFailure("同步异常: " + e.getMessage());
            groupChatMapper.updateById(groupChat);

            // Record failure metric
            metricsService.recordGroupSyncFailure(groupChat.getGroupId(), "exception");

            return GroupSyncResultDTO.failure(groupChat.getId(), groupChat.getGroupName(),
                    "同步异常: " + e.getMessage(), true);
        }
    }

    /**
     * Resilience4j 重试失败后的 fallback 方法
     */
    public GroupSyncResultDTO syncGroupFallback(GroupChat groupChat, Exception e) {
        log.error("群组同步重试失败，执行 fallback: groupId={}, groupName={}",
                groupChat.getGroupId(), groupChat.getGroupName(), e);
        groupChat.markSyncFailure("重试失败: " + e.getMessage());
        groupChatMapper.updateById(groupChat);
        return GroupSyncResultDTO.failure(groupChat.getId(), groupChat.getGroupName(),
                "重试失败: " + e.getMessage(), true);
    }

    @Override
    public BatchSyncResultDTO batchSyncGroups(List<GroupChat> groupChats) {
        log.info("开始批量同步群组: count={}", groupChats.size());
        log.debug("批量同步群组列表: groupIds={}",
            groupChats.stream().map(GroupChat::getGroupId).collect(Collectors.toList()));
        LocalDateTime startTime = LocalDateTime.now();

        List<GroupSyncResultDTO> results = groupChats.stream()
                .map(this::syncGroup)
                .collect(Collectors.toList());

        LocalDateTime endTime = LocalDateTime.now();
        BatchSyncResultDTO batchResult = BatchSyncResultDTO.from(results, startTime, endTime);

        log.info("批量同步完成: total={}, success={}, failure={}, duration={}ms",
                batchResult.totalCount(), batchResult.successCount(),
                batchResult.failureCount(), batchResult.durationMs());
        log.debug("批量同步详细结果: successRate={}%, results={}",
            String.format("%.2f", batchResult.getSuccessRate()),
            results.stream().map(r -> String.format("%s:%s", r.groupName(), r.syncStatus())).collect(Collectors.toList()));

        // Record batch sync duration metric
        metricsService.recordGroupSyncDuration(batchResult.durationMs());

        return batchResult;
    }

    @Override
    public BatchSyncResultDTO syncAllActiveGroups() {
        log.info("开始同步所有活跃群组");
        List<GroupChat> activeGroups = groupChatMapper.selectActiveGroups();
        log.info("查询到活跃群组数量: {}", activeGroups.size());
        return batchSyncGroups(activeGroups);
    }

    @Override
    public BatchSyncResultDTO retryFailedGroups(Integer minFailureCount) {
        log.info("开始重试失败群组: minFailureCount={}", minFailureCount);
        List<GroupChat> failedGroups = groupChatMapper.selectFailedGroups(minFailureCount);
        log.info("查询到失败群组数量: {}", failedGroups.size());
        return batchSyncGroups(failedGroups);
    }

    @Override
    @Transactional
    public Integer discoverNewGroups(Long clientId) {
        log.info("开始自动发现新群组: clientId={}", clientId);

        try {
            ChatClient client = getChatClient(clientId);
            if (client == null) {
                return 0;
            }

            List<Map<String, Object>> groupList = fetchGroupListFromAdapter(client, clientId);
            if (groupList == null || groupList.isEmpty()) {
                return 0;
            }

            List<GroupChat> newGroups = processAndSaveNewGroups(groupList, clientId);

            publishGroupDiscoveryEvent(newGroups, clientId);

            log.info("自动发现完成: clientId={}, newGroups={}", clientId, newGroups.size());
            return newGroups.size();

        } catch (Exception e) {
            log.error("自动发现新群组失败: clientId={}", clientId, e);
            return 0;
        }
    }

    /**
     * 获取客户端信息
     */
    private ChatClient getChatClient(Long clientId) {
        ChatClient client = chatClientMapper.selectById(clientId);
        if (client == null) {
            log.error("客户端不存在: clientId={}", clientId);
        }
        return client;
    }

    /**
     * 从适配器获取群组列表
     */
    private List<Map<String, Object>> fetchGroupListFromAdapter(ChatClient client, Long clientId) {
        log.debug("获取客户端适配器用于群组发现: clientType={}", client.getClientType());
        ClientAdapter adapter = clientAdapterFactory.getAdapter(client.getClientType());
        if (!(adapter instanceof NapCatAdapter napCatAdapter)) {
            throw new IllegalStateException("Only NapCat adapter is supported for discovery");
        }

        log.debug("调用NapCat API获取群组列表");
        CompletableFuture<ApiCallResponseDTO> future = napCatAdapter.getGroupList();
        ApiCallResponseDTO response = future.join();

        if (response == null || response.getRetcode() != 0) {
            log.error("获取群组列表失败: clientId={}, retcode={}",
                    clientId, response != null ? response.getRetcode() : "null");
            return null;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> groupList = (List<Map<String, Object>>) response.getRawData();
        log.debug("NapCat返回群组列表: totalCount={}", groupList != null ? groupList.size() : 0);

        if (groupList == null || groupList.isEmpty()) {
            log.info("未发现新群组: clientId={}", clientId);
        }

        return groupList;
    }

    /**
     * 处理并保存新群组
     */
    private List<GroupChat> processAndSaveNewGroups(List<Map<String, Object>> groupList, Long clientId) {
        List<GroupChat> existingGroups = groupChatMapper.selectList(null);
        List<String> existingGroupIds = existingGroups.stream()
                .map(GroupChat::getGroupId)
                .collect(Collectors.toList());
        log.debug("当前已存在群组数量: {}", existingGroupIds.size());

        List<GroupChat> newGroups = new ArrayList<>();
        for (Map<String, Object> groupData : groupList) {
            String groupId = String.valueOf(groupData.get("group_id"));
            if (!existingGroupIds.contains(groupId)) {
                GroupChat newGroup = createNewGroup(groupData, clientId);
                groupChatMapper.insert(newGroup);
                newGroups.add(newGroup);
                log.info("新增群组: groupId={}, groupName={}", groupId, newGroup.getGroupName());
            }
        }

        return newGroups;
    }

    /**
     * 创建新群组实体
     */
    private GroupChat createNewGroup(Map<String, Object> groupData, Long clientId) {
        GroupChat newGroup = new GroupChat();
        newGroup.setGroupId(String.valueOf(groupData.get("group_id")));
        newGroup.setGroupName((String) groupData.get("group_name"));
        newGroup.setClientId(clientId);
        newGroup.setMemberCount(getIntValue(groupData, "member_count"));
        newGroup.setEnabled(true);
        newGroup.setActive(true);
        newGroup.setSyncStatus(SyncStatus.SUCCESS);
        newGroup.setLastSyncTime(LocalDateTime.now());
        return newGroup;
    }

    /**
     * 发布群组发现事件
     */
    private void publishGroupDiscoveryEvent(List<GroupChat> newGroups, Long clientId) {
        if (!newGroups.isEmpty()) {
            GroupDiscoveryEvent event = new GroupDiscoveryEvent(this, newGroups, clientId);
            eventPublisher.publishEvent(event);
            log.info("发布群组发现事件: clientId={}, newGroupCount={}", clientId, newGroups.size());

            metricsService.recordGroupDiscovery(newGroups.size());
        }
    }

    @Override
    @Transactional
    public void updateSyncStatus(GroupChat groupChat) {
        groupChatMapper.updateById(groupChat);
        log.debug("更新群组同步状态: groupId={}, status={}", groupChat.getGroupId(), groupChat.getSyncStatus());
    }

    @Override
    @Transactional
    public Integer batchUpdateSyncStatus(List<GroupChat> groupChats) {
        if (groupChats == null || groupChats.isEmpty()) {
            return 0;
        }
        int updated = groupChatMapper.batchUpdateSyncStatus(groupChats);
        log.info("批量更新同步状态: count={}, updated={}", groupChats.size(), updated);
        return updated;
    }

    @Override
    public List<GroupChat> getAlertGroups() {
        List<GroupChat> alertGroups = groupChatMapper.selectFailedGroups(SyncConstants.ALERT_FAILURE_THRESHOLD);
        log.info("查询需要告警的群组: count={}", alertGroups.size());
        return alertGroups;
    }

    @Override
    @Transactional
    public void resetFailureCount(Long groupId) {
        GroupChat groupChat = groupChatMapper.selectById(groupId);
        if (groupChat != null) {
            groupChat.resetFailureCount();
            groupChatMapper.updateById(groupChat);
            log.info("重置群组失败计数: groupId={}", groupId);
        }
    }

    /**
     * 安全获取 Integer 值
     */
    private Integer getIntValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            log.warn("无法解析整数值: key={}, value={}", key, value);
            return null;
        }
    }
}
