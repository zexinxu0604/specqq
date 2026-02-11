package com.specqq.chatbot.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * Format Statistics Request DTO
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Data
public class FormatStatisticsRequestDTO {

    /**
     * Character count
     */
    @NotNull(message = "Character count cannot be null")
    @Min(value = 0, message = "Character count cannot be negative")
    private Integer characterCount;

    /**
     * CQ code counts by type
     */
    @NotNull(message = "CQ code counts cannot be null")
    private Map<String, Integer> cqCodeCounts;
}
