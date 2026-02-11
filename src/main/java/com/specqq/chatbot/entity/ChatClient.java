package com.specqq.chatbot.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天客户端实体类
 *
 * @author Chatbot Router System
 * @tableName chat_client
 */
@Data
@TableName(value = "chat_client", autoResultMap = true)
public class ChatClient {

    /**
     * 客户端唯一标识
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 客户端类型 (qq/wechat/dingtalk)
     */
    @TableField("client_type")
    private String clientType;

    /**
     * 客户端名称(用于展示)
     */
    @TableField("client_name")
    private String clientName;

    /**
     * 通信协议 (websocket/http/both)
     */
    @TableField("protocol_type")
    private String protocolType;

    /**
     * 连接配置(JSON格式)
     */
    @TableField(value = "connection_config", typeHandler = JacksonTypeHandler.class)
    private ConnectionConfig connectionConfig;

    /**
     * 连接状态 (connected/disconnected/connecting/error)
     */
    @TableField("connection_status")
    private String connectionStatus;

    /**
     * 最后心跳时间
     */
    @TableField("last_heartbeat_time")
    private LocalDateTime lastHeartbeatTime;

    /**
     * 是否启用
     */
    @TableField("enabled")
    private Boolean enabled;

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
     * 连接配置内部类
     */
    @Data
    public static class ConnectionConfig {
        private String host;
        private Integer wsPort;
        private Integer httpPort;
        private String accessToken;
        private Integer reconnectInterval;
    }
}
