package com.specqq.chatbot.parser;

import com.specqq.chatbot.common.CQCodeConstants;

/**
 * CQ Code Type Enumeration
 *
 * <p>Defines standard CQ code types according to OneBot 11 specification.
 * Each type has a corresponding Chinese label and unit suffix for display purposes.</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
public enum CQCodeType {

    /**
     * Face/Emoji (表情)
     * Example: [CQ:face,id=123]
     */
    FACE(CQCodeConstants.TYPE_FACE, CQCodeConstants.LABEL_FACE, CQCodeConstants.UNIT_FACE),

    /**
     * Image (图片)
     * Example: [CQ:image,file=abc.jpg,url=https://...]
     */
    IMAGE(CQCodeConstants.TYPE_IMAGE, CQCodeConstants.LABEL_IMAGE, CQCodeConstants.UNIT_IMAGE),

    /**
     * At/Mention (@提及)
     * Example: [CQ:at,qq=123456]
     */
    AT(CQCodeConstants.TYPE_AT, CQCodeConstants.LABEL_AT, CQCodeConstants.UNIT_AT),

    /**
     * Reply (回复)
     * Example: [CQ:reply,id=123456]
     */
    REPLY(CQCodeConstants.TYPE_REPLY, CQCodeConstants.LABEL_REPLY, CQCodeConstants.UNIT_REPLY),

    /**
     * Voice/Record (语音)
     * Example: [CQ:record,file=abc.mp3]
     */
    RECORD(CQCodeConstants.TYPE_RECORD, CQCodeConstants.LABEL_RECORD, CQCodeConstants.UNIT_RECORD),

    /**
     * Video (视频)
     * Example: [CQ:video,file=abc.mp4]
     */
    VIDEO(CQCodeConstants.TYPE_VIDEO, CQCodeConstants.LABEL_VIDEO, CQCodeConstants.UNIT_VIDEO),

    /**
     * Other/Unknown (其他)
     * Used for CQ codes that don't match known types
     */
    OTHER(CQCodeConstants.TYPE_OTHER, "其他", "个");

    private final String code;
    private final String label;
    private final String unit;

    /**
     * Constructor
     *
     * @param code  CQ code type string (e.g., "face", "image")
     * @param label Chinese label for display (e.g., "表情", "图片")
     * @param unit  Chinese unit suffix for count display (e.g., "个", "张")
     */
    CQCodeType(String code, String label, String unit) {
        this.code = code;
        this.label = label;
        this.unit = unit;
    }

    /**
     * Get CQ code type string
     *
     * @return Type code (e.g., "face", "image")
     */
    public String getCode() {
        return code;
    }

    /**
     * Get Chinese label for display
     *
     * @return Chinese label (e.g., "表情", "图片")
     */
    public String getLabel() {
        return label;
    }

    /**
     * Get Chinese unit suffix for count display
     *
     * @return Unit suffix (e.g., "个", "张")
     */
    public String getUnit() {
        return unit;
    }

    /**
     * Get CQCodeType from code string
     *
     * @param code CQ code type string (e.g., "face", "image")
     * @return Corresponding CQCodeType enum, or OTHER if not found
     */
    public static CQCodeType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return OTHER;
        }

        for (CQCodeType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }

        return OTHER;
    }

    /**
     * Check if this type is a known standard type (not OTHER)
     *
     * @return true if standard type, false if OTHER
     */
    public boolean isStandardType() {
        return this != OTHER;
    }

    @Override
    public String toString() {
        return String.format("%s(%s, %s)", code, label, unit);
    }
}
