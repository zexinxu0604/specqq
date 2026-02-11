package com.specqq.chatbot.common;

/**
 * CQ Code Type Constants
 *
 * <p>Defines standard CQ code types according to OneBot 11 specification.
 * Used for parsing, validation, and statistics calculation.</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
public final class CQCodeConstants {

    private CQCodeConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ========== CQ Code Types ==========

    /**
     * Face/Emoji CQ code type
     * Example: [CQ:face,id=123]
     */
    public static final String TYPE_FACE = "face";

    /**
     * Image CQ code type
     * Example: [CQ:image,file=abc.jpg,url=https://...]
     */
    public static final String TYPE_IMAGE = "image";

    /**
     * At/Mention CQ code type
     * Example: [CQ:at,qq=123456]
     */
    public static final String TYPE_AT = "at";

    /**
     * Reply CQ code type
     * Example: [CQ:reply,id=123456]
     */
    public static final String TYPE_REPLY = "reply";

    /**
     * Voice/Record CQ code type
     * Example: [CQ:record,file=abc.mp3]
     */
    public static final String TYPE_RECORD = "record";

    /**
     * Video CQ code type
     * Example: [CQ:video,file=abc.mp4]
     */
    public static final String TYPE_VIDEO = "video";

    /**
     * Other/Unknown CQ code type
     * Used for CQ codes that don't match known types
     */
    public static final String TYPE_OTHER = "other";

    // ========== Chinese Labels for Display ==========

    /**
     * Chinese label for face/emoji
     */
    public static final String LABEL_FACE = "表情";

    /**
     * Chinese label for image
     */
    public static final String LABEL_IMAGE = "图片";

    /**
     * Chinese label for at/mention
     */
    public static final String LABEL_AT = "@提及";

    /**
     * Chinese label for reply
     */
    public static final String LABEL_REPLY = "回复";

    /**
     * Chinese label for voice/record
     */
    public static final String LABEL_RECORD = "语音";

    /**
     * Chinese label for video
     */
    public static final String LABEL_VIDEO = "视频";

    /**
     * Chinese label for text/characters
     */
    public static final String LABEL_TEXT = "文字";

    // ========== Unit Suffixes for Display ==========

    /**
     * Unit suffix for face/emoji count
     */
    public static final String UNIT_FACE = "个";

    /**
     * Unit suffix for image count
     */
    public static final String UNIT_IMAGE = "张";

    /**
     * Unit suffix for at/mention count
     */
    public static final String UNIT_AT = "个";

    /**
     * Unit suffix for reply count
     */
    public static final String UNIT_REPLY = "条";

    /**
     * Unit suffix for voice/record count
     */
    public static final String UNIT_RECORD = "条";

    /**
     * Unit suffix for video count
     */
    public static final String UNIT_VIDEO = "个";

    /**
     * Unit suffix for text character count
     */
    public static final String UNIT_TEXT = "字";

    // ========== CQ Code Regex Pattern ==========

    /**
     * Regex pattern for matching CQ codes
     * Pattern: [CQ:type,param1=value1,param2=value2]
     * Groups: 1=type, 2=params (optional)
     */
    public static final String CQ_CODE_PATTERN = "\\[CQ:([a-z_]+)(?:,([^\\]]+))?\\]";

    // ========== Cache Configuration ==========

    /**
     * Caffeine cache name for compiled CQ code patterns
     */
    public static final String CACHE_CQ_PATTERNS = "cqCodePatterns";

    /**
     * Caffeine cache name for statistics rules
     */
    public static final String CACHE_STATISTICS_RULES = "statisticsRules";

    /**
     * Cache TTL in hours for compiled patterns
     */
    public static final long CACHE_TTL_HOURS = 4;

    /**
     * Maximum cache size for compiled patterns
     */
    public static final int CACHE_MAX_SIZE = 100;

    // ========== Rate Limiting ==========

    /**
     * Redis key prefix for statistics rate limiting
     * Format: rate_limit:statistics:{group_id}
     */
    public static final String REDIS_RATE_LIMIT_PREFIX = "rate_limit:statistics:";

    /**
     * Rate limit window in seconds (5 seconds)
     */
    public static final int RATE_LIMIT_WINDOW_SECONDS = 5;

    /**
     * Maximum statistics replies per window
     */
    public static final int RATE_LIMIT_MAX_REPLIES = 1;

    // ========== Performance Targets ==========

    /**
     * Target P95 latency for CQ code parsing (milliseconds)
     */
    public static final int TARGET_PARSE_P95_MS = 10;

    /**
     * Target P95 latency for statistics calculation (milliseconds)
     */
    public static final int TARGET_STATISTICS_P95_MS = 50;

    /**
     * Target cache hit rate (percentage)
     */
    public static final double TARGET_CACHE_HIT_RATE = 0.95;
}
