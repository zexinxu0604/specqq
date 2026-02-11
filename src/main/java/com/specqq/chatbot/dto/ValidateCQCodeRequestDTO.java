package com.specqq.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Validate CQ Code Request DTO
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Data
public class ValidateCQCodeRequestDTO {

    /**
     * CQ code string to validate
     */
    @NotBlank(message = "CQ code cannot be blank")
    private String cqCode;
}
