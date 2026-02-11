package com.specqq.chatbot.vo;

import lombok.Data;

import java.util.Map;

/**
 * CQ Code View Object
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Data
public class CQCodeVO {

    /**
     * CQ code type (e.g., "face", "image", "at")
     */
    private String type;

    /**
     * CQ code parameters
     */
    private Map<String, String> params;

    /**
     * Original CQ code text
     */
    private String rawText;

    /**
     * Chinese label (e.g., "表情", "图片")
     */
    private String label;

    /**
     * Chinese unit suffix (e.g., "个", "张")
     */
    private String unit;
}
