package com.specqq.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CQ Code Pattern Data Transfer Object
 *
 * <p>Represents a CQ code pattern for UI dropdown selection.</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CQCodePatternDTO {

    /**
     * CQ code type (face, image, at, reply, record, video)
     */
    private String type;

    /**
     * Chinese label for display (表情, 图片, @提及, etc.)
     */
    private String label;

    /**
     * Regex pattern for matching
     */
    private String regexPattern;
}
