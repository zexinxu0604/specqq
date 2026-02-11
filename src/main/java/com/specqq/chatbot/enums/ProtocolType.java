package com.specqq.chatbot.enums;

/**
 * 协议类型枚举
 *
 * @author Chatbot Router System
 */
public enum ProtocolType {
    /**
     * WebSocket协议
     */
    WEBSOCKET("WebSocket", "ws://"),

    /**
     * HTTP协议
     */
    HTTP("HTTP", "http://"),

    /**
     * HTTPS协议
     */
    HTTPS("HTTPS", "https://"),

    /**
     * gRPC协议
     */
    GRPC("gRPC", "grpc://");

    private final String name;
    private final String prefix;

    ProtocolType(String name, String prefix) {
        this.name = name;
        this.prefix = prefix;
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }
}
