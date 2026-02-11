package com.specqq.chatbot.service;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Message Statistics
 *
 * <p>Immutable record containing message statistics:
 * <ul>
 *   <li>Character count (excluding CQ codes, by Unicode code points)</li>
 *   <li>CQ code counts grouped by type (face, image, at, reply, record, video, etc.)</li>
 * </ul>
 * </p>
 *
 * <p>Example:
 * <pre>
 * Message: "Hello[CQ:face,id=1]你好[CQ:image,file=a.jpg]"
 * Statistics: MessageStatistics(8, {"face": 1, "image": 1})
 * </pre>
 * </p>
 *
 * @param characterCount Character count (by Unicode code points, not bytes)
 * @param cqCodeCounts   Map of CQ code type to count (e.g., {"face": 3, "image": 2})
 * @author Claude Code
 * @since 2026-02-11
 */
public record MessageStatistics(
        int characterCount,
        Map<String, Integer> cqCodeCounts
) {

    /**
     * Canonical constructor with validation
     *
     * @param characterCount Character count (must be non-negative)
     * @param cqCodeCounts   CQ code counts map (must not be null, will be made immutable)
     * @throws IllegalArgumentException if characterCount is negative or cqCodeCounts is null
     */
    public MessageStatistics {
        if (characterCount < 0) {
            throw new IllegalArgumentException("Character count cannot be negative");
        }
        Objects.requireNonNull(cqCodeCounts, "CQ code counts map cannot be null");

        // Make cqCodeCounts immutable to enforce record immutability
        cqCodeCounts = Collections.unmodifiableMap(cqCodeCounts);
    }

    /**
     * Get count for a specific CQ code type
     *
     * @param type CQ code type (e.g., "face", "image")
     * @return Count for that type, or 0 if type not present
     */
    public int getCountForType(String type) {
        return cqCodeCounts.getOrDefault(type, 0);
    }

    /**
     * Get total number of CQ codes across all types
     *
     * @return Total CQ code count
     */
    public int getTotalCQCodeCount() {
        return cqCodeCounts.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    /**
     * Check if message has any CQ codes
     *
     * @return true if at least one CQ code, false otherwise
     */
    public boolean hasCQCodes() {
        return !cqCodeCounts.isEmpty() && getTotalCQCodeCount() > 0;
    }

    /**
     * Check if message has any text characters
     *
     * @return true if character count > 0, false otherwise
     */
    public boolean hasText() {
        return characterCount > 0;
    }

    /**
     * Check if message is empty (no text and no CQ codes)
     *
     * @return true if empty, false otherwise
     */
    public boolean isEmpty() {
        return characterCount == 0 && !hasCQCodes();
    }

    /**
     * Get number of distinct CQ code types present
     *
     * @return Number of distinct types with count > 0
     */
    public int getDistinctCQCodeTypeCount() {
        return (int) cqCodeCounts.values().stream()
                .filter(count -> count > 0)
                .count();
    }

    @Override
    public String toString() {
        return String.format("MessageStatistics{characterCount=%d, cqCodeCounts=%s, totalCQCodes=%d}",
                characterCount, cqCodeCounts, getTotalCQCodeCount());
    }
}
