package com.specqq.chatbot.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Handler 参数 VO
 *
 * <p>描述 Handler 的单个参数配置</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Handler 参数")
public class HandlerParamVO {

    /**
     * 参数名称（代码中使用）
     */
    @Schema(description = "参数名称", example = "prefix")
    private String name;

    /**
     * 参数显示名称（前端显示）
     */
    @Schema(description = "参数显示名称", example = "前缀")
    private String displayName;

    /**
     * 参数类型
     */
    @Schema(description = "参数类型", example = "string")
    private String type;

    /**
     * 是否必填
     */
    @Schema(description = "是否必填", example = "false")
    private Boolean required;

    /**
     * 默认值
     */
    @Schema(description = "默认值", example = "Echo: ")
    private String defaultValue;

    /**
     * 参数描述
     */
    @Schema(description = "参数描述", example = "添加到消息前的前缀文本")
    private String description;

    /**
     * 枚举值列表（如果参数是枚举类型）
     */
    @Schema(description = "枚举值列表")
    private List<String> enumValues;
}
