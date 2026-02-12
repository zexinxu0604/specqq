package com.specqq.chatbot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模式验证响应 DTO
 *
 * @author Claude Code
 * @since 2026-02-12
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "模式验证响应")
public class ValidatePatternResponseDTO {

    /**
     * 验证是否通过
     */
    @Schema(description = "验证是否通过", example = "true")
    private boolean valid;

    /**
     * 验证消息
     */
    @Schema(description = "验证消息", example = "✓ 正则表达式语法正确")
    private String message;

    /**
     * 创建成功响应
     */
    public static ValidatePatternResponseDTO success(String message) {
        return new ValidatePatternResponseDTO(true, message);
    }

    /**
     * 创建失败响应
     */
    public static ValidatePatternResponseDTO error(String message) {
        return new ValidatePatternResponseDTO(false, message);
    }
}
