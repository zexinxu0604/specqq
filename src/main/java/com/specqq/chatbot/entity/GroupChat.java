package com.specqq.chatbot.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.specqq.chatbot.enums.SyncStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 群聊实体类
 *
 * @author Chatbot Router System
 * @tableName group_chat
 */
@Data
@TableName(value = "group_chat", autoResultMap = true)
public class GroupChat {

    /**
     * 群聊唯一标识(内部ID)
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 群聊平台ID(如QQ群号)
     */
    @TableField("group_id")
    private String groupId;

    /**
     * 群名称
     */
    @TableField("group_name")
    private String groupName;

    /**
     * 所属客户端ID
     */
    @TableField("client_id")
    private Long clientId;

    /**
     * 群成员数量
     */
    @TableField("member_count")
    private Integer memberCount;

    /**
     * 是否启用机器人
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 群配置(JSON格式)
     */
    @TableField(value = "config", typeHandler = JacksonTypeHandler.class)
    private GroupConfig config;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // === 同步状态字段 (V7 Migration) ===

    /**
     * 最后成功同步时间
     */
    @TableField("last_sync_time")
    private LocalDateTime lastSyncTime;

    /**
     * 同步状态: SUCCESS, FAILED
     */
    @TableField("sync_status")
    private SyncStatus syncStatus;

    /**
     * 最后失败时间
     */
    @TableField("last_failure_time")
    private LocalDateTime lastFailureTime;

    /**
     * 失败原因（最大500字符）
     */
    @TableField("failure_reason")
    private String failureReason;

    /**
     * 连续失败次数
     */
    @TableField("consecutive_failure_count")
    private Integer consecutiveFailureCount;

    /**
     * 机器人是否仍在群组中
     */
    @TableField("active")
    private Boolean active;

    /**
     * 所属客户端(非数据库字段)
     */
    @TableField(exist = false)
    private ChatClient client;

    /**
     * 启用的规则列表(非数据库字段)
     */
    @TableField(exist = false)
    private List<MessageRule> enabledRules;

    /**
     * 群配置内部类
     */
    @Data
    public static class GroupConfig {
        private Integer maxMessagesPerMinute = 20;
        private Integer cooldownSeconds = 5;
        private List<String> allowedCommands;
        private List<String> blacklistedWords;
    }

    // === 业务方法 (Sync Management) ===

    /**
     * 标记同步成功
     * 重置失败计数和失败原因
     */
    public void markSyncSuccess() {
        this.lastSyncTime = LocalDateTime.now();
        this.syncStatus = SyncStatus.SUCCESS;
        this.consecutiveFailureCount = 0;
        this.failureReason = null;
    }

    /**
     * 标记同步失败
     *
     * @param reason 失败原因
     */
    public void markSyncFailure(String reason) {
        this.lastFailureTime = LocalDateTime.now();
        this.syncStatus = SyncStatus.FAILED;
        this.failureReason = reason;
        this.consecutiveFailureCount = (this.consecutiveFailureCount == null ? 0 : this.consecutiveFailureCount) + 1;
    }

    /**
     * 重置失败计数（用于手动干预后）
     */
    public void resetFailureCount() {
        this.consecutiveFailureCount = 0;
        this.failureReason = null;
    }

    /**
     * 判断是否需要告警（连续失败3次以上）
     *
     * @return true 如果需要告警
     */
    public boolean needsAlert() {
        return this.consecutiveFailureCount != null && this.consecutiveFailureCount >= 3;
    }
}
