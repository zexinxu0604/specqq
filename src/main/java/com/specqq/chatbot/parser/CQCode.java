package com.specqq.chatbot.parser;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Parsed CQ Code Element
 *
 * <p>Immutable record representing a CQ code parsed from message text.
 * CQ codes are special formatting codes used in OneBot 11 protocol for rich media content.</p>
 *
 * <p>Example: {@code [CQ:face,id=123]} → {@code CQCode("face", {"id": "123"}, "[CQ:face,id=123]")}</p>
 *
 * @param type     CQ code type (e.g., "face", "image", "at", "reply", "record", "video")
 * @param params   Key-value parameters extracted from the CQ code (immutable map)
 * @param rawText  Original CQ code text from message (for logging and debugging)
 * @author Claude Code
 * @since 2026-02-11
 */
public record CQCode(
        String type,
        Map<String, String> params,
        String rawText
) {

    /**
     * Canonical constructor with validation
     *
     * @param type    CQ code type (must not be null or empty)
     * @param params  Parameters map (must not be null, will be made immutable)
     * @param rawText Original CQ code text (must not be null)
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public CQCode {
        Objects.requireNonNull(type, "CQ code type cannot be null");
        Objects.requireNonNull(params, "CQ code params cannot be null");
        Objects.requireNonNull(rawText, "CQ code rawText cannot be null");

        if (type.isBlank()) {
            throw new IllegalArgumentException("CQ code type cannot be empty");
        }

        // Make params immutable to enforce record immutability
        params = Collections.unmodifiableMap(params);
    }

    /**
     * Get parameter value by key
     *
     * @param key Parameter key to look up
     * @return Parameter value, or null if key doesn't exist
     */
    public String getParam(String key) {
        return params.get(key);
    }

    /**
     * Check if parameter exists
     *
     * @param key Parameter key to check
     * @return true if parameter exists, false otherwise
     */
    public boolean hasParam(String key) {
        return params.containsKey(key);
    }

    /**
     * Get parameter value with default fallback
     *
     * @param key          Parameter key to look up
     * @param defaultValue Default value if key doesn't exist
     * @return Parameter value, or defaultValue if key doesn't exist
     */
    public String getParamOrDefault(String key, String defaultValue) {
        return params.getOrDefault(key, defaultValue);
    }

    /**
     * Get number of parameters
     *
     * @return Parameter count
     */
    public int getParamCount() {
        return params.size();
    }

    /**
     * Check if this CQ code has no parameters
     *
     * @return true if no parameters, false otherwise
     */
    public boolean hasNoParams() {
        return params.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("CQCode{type='%s', params=%s, rawText='%s'}",
                type, params, rawText);
    }
}
