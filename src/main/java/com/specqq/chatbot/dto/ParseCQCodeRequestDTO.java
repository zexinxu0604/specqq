package com.specqq.chatbot.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Parse CQ Code Request DTO
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Data
public class ParseCQCodeRequestDTO {

    /**
     * Message content to parse (max 10000 characters)
     */
    @NotNull(message = "Message cannot be null")
    @Size(max = 10000, message = "Message length cannot exceed 10000 characters")
    private String message;
}
