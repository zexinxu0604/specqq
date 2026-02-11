package com.specqq.chatbot.vo;

import lombok.Data;

import java.util.Map;

/**
 * Message Statistics View Object
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Data
public class MessageStatisticsVO {

    /**
     * Character count (by Unicode code points)
     */
    private Integer characterCount;

    /**
     * CQ code counts by type
     */
    private Map<String, Integer> cqCodeCounts;

    /**
     * Total CQ code count across all types
     */
    private Integer totalCQCodeCount;

    /**
     * Whether message has text
     */
    private Boolean hasText;

    /**
     * Whether message has CQ codes
     */
    private Boolean hasCQCodes;
}
