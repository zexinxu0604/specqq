package com.specqq.chatbot.common;

import lombok.Getter;

/**
 * 响应状态码枚举
 *
 * @author Chatbot Router System
 */
@Getter
public enum ResultCode {

    // ==================== 成功状态码 2xx ====================
    SUCCESS(200, "操作成功"),
    CREATED(201, "创建成功"),
    ACCEPTED(202, "请求已接受"),
    NO_CONTENT(204, "无内容"),

    // ==================== 客户端错误 4xx ====================
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证，请登录"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    CONFLICT(409, "资源冲突"),
    UNPROCESSABLE_ENTITY(422, "请求参数验证失败"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),

    // ==================== 服务器错误 5xx ====================
    INTERNAL_ERROR(500, "服务器内部错误"),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    NOT_IMPLEMENTED(501, "功能未实现"),
    BAD_GATEWAY(502, "网关错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),
    GATEWAY_TIMEOUT(504, "网关超时"),

    // ==================== 业务错误码 1xxx ====================
    // 规则相关 10xx
    RULE_NOT_FOUND(1001, "规则不存在"),
    RULE_NAME_DUPLICATE(1002, "规则名称已存在"),
    RULE_PATTERN_INVALID(1003, "规则匹配模式无效"),
    RULE_IN_USE(1004, "规则正在使用中，无法删除"),
    RULE_DISABLED(1005, "规则已禁用"),

    // 群聊相关 11xx
    GROUP_NOT_FOUND(1101, "群聊不存在"),
    GROUP_DISABLED(1102, "群聊已禁用"),
    GROUP_CONFIG_INVALID(1103, "群聊配置无效"),

    // 客户端相关 12xx
    CLIENT_NOT_FOUND(1201, "客户端不存在"),
    CLIENT_DISCONNECTED(1202, "客户端未连接"),
    CLIENT_CONFIG_INVALID(1203, "客户端配置无效"),
    CLIENT_IN_USE(1204, "客户端正在使用中，无法删除"),

    // 用户相关 13xx
    USER_NOT_FOUND(1301, "用户不存在"),
    USER_DISABLED(1302, "用户已禁用"),
    USERNAME_DUPLICATE(1303, "用户名已存在"),
    PASSWORD_INCORRECT(1304, "密码错误"),
    TOKEN_EXPIRED(1305, "登录已过期，请重新登录"),
    TOKEN_INVALID(1306, "无效的登录凭证"),
    AUTH_FAILED(1307, "认证失败"),

    // 日志相关 14xx
    LOG_NOT_FOUND(1401, "日志不存在"),
    LOG_EXPORT_FAILED(1402, "日志导出失败"),

    // 消息相关 15xx
    MESSAGE_SEND_FAILED(1501, "消息发送失败"),
    MESSAGE_RATE_LIMITED(1502, "消息发送频率超限"),
    MESSAGE_CONTENT_INVALID(1503, "消息内容无效"),

    // 缓存相关 16xx
    CACHE_ERROR(1601, "缓存操作失败"),

    // 数据库相关 17xx
    DATABASE_ERROR(1701, "数据库操作失败"),
    DATA_INTEGRITY_VIOLATION(1702, "数据完整性约束冲突"),
    OPTIMISTIC_LOCK_FAILURE(1703, "数据已被修改，请刷新后重试");

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 状态消息
     */
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 根据状态码获取枚举
     */
    public static ResultCode getByCode(Integer code) {
        for (ResultCode resultCode : ResultCode.values()) {
            if (resultCode.getCode().equals(code)) {
                return resultCode;
            }
        }
        return INTERNAL_SERVER_ERROR;
    }
}
