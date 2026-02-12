package com.specqq.chatbot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统配置实体
 * 用于存储系统级配置，包括默认规则配置
 *
 * @author Claude Code
 * @since 2026-02-12
 */
@Data
@TableName("system_config")
public class SystemConfig {

    /**
     * 配置唯一标识
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 配置键（唯一索引）
     */
    private String configKey;

    /**
     * 配置值（JSON格式）
     * 使用 JacksonTypeHandler 自动序列化/反序列化
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Object configValue;

    /**
     * 配置类型（用于分类）
     */
    private String configType;

    /**
     * 配置描述
     */
    private String description;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
