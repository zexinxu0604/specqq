package com.specqq.chatbot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Handler 链实体类
 *
 * @author Claude Code
 * @since 2026-02-11
 * @tableName handler_chain
 */
@Data
@TableName(value = "handler_chain")
public class HandlerChain {

    /**
     * Handler 链 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 规则 ID（外键）
     */
    @TableField("rule_id")
    private Long ruleId;

    /**
     * Handler 类型标识（对应 @HandlerMetadata.handlerType）
     */
    @TableField("handler_type")
    private String handlerType;

    /**
     * Handler 配置参数（JSON 格式）
     */
    @TableField("handler_config")
    private String handlerConfig;

    /**
     * 执行顺序（数字越小越先执行）
     */
    @TableField("sequence_order")
    private Integer sequenceOrder;

    /**
     * 是否启用
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
