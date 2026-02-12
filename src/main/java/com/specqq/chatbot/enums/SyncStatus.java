package com.specqq.chatbot.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 同步状态枚举
 * 用于标识群组同步操作的结果状态
 *
 * @author Claude Code
 * @since 2026-02-12
 */
@Getter
public enum SyncStatus {

    /**
     * 同步成功
     */
    SUCCESS("SUCCESS", "同步成功"),

    /**
     * 同步失败
     */
    FAILED("FAILED", "同步失败");

    /**
     * 数据库存储值
     */
    @EnumValue
    @JsonValue
    private final String code;

    /**
     * 状态描述
     */
    private final String description;

    /**
     * 构造函数
     *
     * @param code        状态代码
     * @param description 状态描述
     */
    SyncStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
