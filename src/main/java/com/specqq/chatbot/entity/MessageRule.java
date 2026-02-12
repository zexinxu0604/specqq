package com.specqq.chatbot.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.specqq.chatbot.common.enums.OnErrorPolicy;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息规则实体类
 *
 * @author Chatbot Router System
 * @tableName message_rule
 */
@Data
@TableName(value = "message_rule")
public class MessageRule {

    /**
     * 规则唯一标识
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 规则名称(唯一)
     */
    @TableField("name")
    private String name;

    /**
     * 规则描述
     */
    @TableField("description")
    private String description;

    /**
     * 匹配类型 (exact/contains/regex)
     */
    @TableField("match_type")
    private MatchType matchType;

    /**
     * 匹配模式(关键词或正则表达式)
     */
    @TableField("pattern")
    private String pattern;

    /**
     * 回复模板(支持变量: {user}, {group}, {time})
     */
    @TableField("response_template")
    private String responseTemplate;

    /**
     * Handler 配置（JSON 格式）
     */
    @TableField("handler_config")
    private String handlerConfig;

    /**
     * 错误策略: STOP, CONTINUE, LOG_ONLY
     */
    @TableField("on_error_policy")
    private OnErrorPolicy onErrorPolicy;

    /**
     * 优先级(1-1000, 数字越小优先级越高)
     */
    @TableField("priority")
    private Integer priority;

    /**
     * 是否启用
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 逻辑删除标志
     */
    @TableLogic
    @TableField("deleted")
    private Boolean deleted;

    /**
     * 创建人
     */
    @TableField("create_by")
    private String createBy;

    /**
     * 更新人
     */
    @TableField("update_by")
    private String updateBy;

    /**
     * 创建者ID(外键)
     */
    @TableField("created_by")
    private Long createdBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 匹配类型枚举
     */
    public enum MatchType {
        /**
         * 精确匹配(区分大小写)
         */
        EXACT,

        /**
         * 包含匹配(不区分大小写)
         */
        CONTAINS,

        /**
         * 正则表达式匹配
         */
        REGEX,

        /**
         * 前缀匹配
         */
        PREFIX,

        /**
         * 后缀匹配
         */
        SUFFIX,

        /**
         * 消息统计规则(自动匹配所有消息)
         * 用于CQ码解析和消息统计功能
         */
        STATISTICS
    }
}
