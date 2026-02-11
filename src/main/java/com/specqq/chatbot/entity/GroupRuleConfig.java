package com.specqq.chatbot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 群聊规则配置实体类(多对多关系表)
 *
 * @author Chatbot Router System
 * @tableName group_rule_config
 */
@Data
@TableName(value = "group_rule_config")
public class GroupRuleConfig {

    /**
     * 配置唯一标识
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 群聊ID(外键)
     */
    @TableField("group_id")
    private Long groupId;

    /**
     * 规则ID(外键)
     */
    @TableField("rule_id")
    private Long ruleId;

    /**
     * 是否启用(群级别开关)
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 执行次数统计
     */
    @TableField("execution_count")
    private Long executionCount;

    /**
     * 最后执行时间
     */
    @TableField("last_executed_at")
    private LocalDateTime lastExecutedAt;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 规则对象(非数据库字段)
     */
    @TableField(exist = false)
    private MessageRule rule;

    /**
     * 群聊对象(非数据库字段)
     */
    @TableField(exist = false)
    private GroupChat group;
}
