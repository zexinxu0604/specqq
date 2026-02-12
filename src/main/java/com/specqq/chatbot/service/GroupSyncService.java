package com.specqq.chatbot.service;

import com.specqq.chatbot.dto.BatchSyncResultDTO;
import com.specqq.chatbot.dto.GroupSyncResultDTO;
import com.specqq.chatbot.entity.GroupChat;

import java.util.List;

/**
 * 群组同步服务接口
 * 提供群组信息同步、状态管理和失败重试功能
 *
 * @author Claude Code
 * @since 2026-02-12
 */
public interface GroupSyncService {

    /**
     * 同步单个群组信息
     *
     * @param groupChat 群聊实体
     * @return 同步结果
     */
    GroupSyncResultDTO syncGroup(GroupChat groupChat);

    /**
     * 批量同步群组信息
     *
     * @param groupChats 群聊实体列表
     * @return 批量同步结果
     */
    BatchSyncResultDTO batchSyncGroups(List<GroupChat> groupChats);

    /**
     * 同步所有活跃群组
     * 由定时任务调用
     *
     * @return 批量同步结果
     */
    BatchSyncResultDTO syncAllActiveGroups();

    /**
     * 重试失败的群组同步
     * 对连续失败次数 >= minFailureCount 的群组进行重试
     *
     * @param minFailureCount 最小失败次数阈值
     * @return 批量同步结果
     */
    BatchSyncResultDTO retryFailedGroups(Integer minFailureCount);

    /**
     * 自动发现并添加新群组
     * 从 NapCat 获取机器人所在的所有群组，添加不存在的群组
     *
     * @param clientId 客户端ID
     * @return 新增群组数量
     */
    Integer discoverNewGroups(Long clientId);

    /**
     * 更新群组同步状态
     *
     * @param groupChat 群聊实体（包含更新后的同步状态）
     */
    void updateSyncStatus(GroupChat groupChat);

    /**
     * 批量更新群组同步状态
     *
     * @param groupChats 群聊实体列表
     * @return 更新记录数
     */
    Integer batchUpdateSyncStatus(List<GroupChat> groupChats);

    /**
     * 获取需要告警的失败群组
     * 连续失败次数 >= 3 的群组
     *
     * @return 失败群组列表
     */
    List<GroupChat> getAlertGroups();

    /**
     * 重置群组失败计数
     * 用于手动干预后重置失败状态
     *
     * @param groupId 群组ID
     */
    void resetFailureCount(Long groupId);
}
