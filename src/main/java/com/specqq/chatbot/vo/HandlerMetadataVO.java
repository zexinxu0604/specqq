package com.specqq.chatbot.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Handler 元数据 VO
 *
 * <p>用于前端展示 Handler 信息和参数配置</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Handler 元数据")
public class HandlerMetadataVO {

    /**
     * Handler 类型标识
     */
    @Schema(description = "Handler 类型标识", example = "ECHO")
    private String handlerType;

    /**
     * Handler 名称
     */
    @Schema(description = "Handler 名称", example = "回声处理器")
    private String name;

    /**
     * Handler 描述
     */
    @Schema(description = "Handler 描述", example = "将接收到的消息加上指定前缀后返回")
    private String description;

    /**
     * Handler 分类
     */
    @Schema(description = "Handler 分类", example = "测试工具")
    private String category;

    /**
     * 是否启用
     */
    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;

    /**
     * 参数列表
     */
    @Schema(description = "参数列表")
    private List<HandlerParamVO> params;
}
