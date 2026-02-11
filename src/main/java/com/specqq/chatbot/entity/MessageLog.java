package com.specqq.chatbot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息日志实体类
 *
 * @author Chatbot Router System
 * @tableName message_log
 * @note 按季度分区(PARTITION BY RANGE (YEAR(timestamp) * 100 + QUARTER(timestamp)))
 */
@Data
@TableName(value = "message_log")
public class MessageLog {

    /**
     * 日志唯一标识
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 消息ID(来自聊天平台)
     */
    @TableField("message_id")
    private String messageId;

    /**
     * 群聊ID(外键)
     */
    @TableField("group_id")
    private Long groupId;

    /**
     * 用户ID(来自聊天平台)
     */
    @TableField("user_id")
    private String userId;

    /**
     * 用户昵称
     */
    @TableField("user_nickname")
    private String userNickname;

    /**
     * 消息内容(TEXT类型)
     */
    @TableField("message_content")
    private String messageContent;

    /**
     * 匹配的规则ID(外键, 可为NULL)
     */
    @TableField("matched_rule_id")
    private Long matchedRuleId;

    /**
     * 回复内容(TEXT类型)
     */
    @TableField("response_content")
    private String responseContent;

    /**
     * 处理耗时(毫秒)
     */
    @TableField("processing_time_ms")
    private Integer processingTimeMs;

    /**
     * 发送状态
     */
    @TableField("send_status")
    private SendStatus sendStatus;

    /**
     * 错误信息(失败时记录)
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 消息时间戳
     */
    @TableField("timestamp")
    private LocalDateTime timestamp;

    /**
     * 匹配的规则对象(非数据库字段)
     */
    @TableField(exist = false)
    private MessageRule matchedRule;

    /**
     * 群聊对象(非数据库字段)
     */
    @TableField(exist = false)
    private GroupChat group;

    /**
     * 发送状态枚举
     */
    public enum SendStatus {
        /**
         * 发送成功
         */
        SUCCESS,

        /**
         * 发送失败
         */
        FAILED,

        /**
         * 等待发送
         */
        PENDING,

        /**
         * 跳过处理(频率限制/规则未匹配)
         */
        SKIPPED
    }
}
