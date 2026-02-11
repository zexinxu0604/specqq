package com.specqq.chatbot.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
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
}
