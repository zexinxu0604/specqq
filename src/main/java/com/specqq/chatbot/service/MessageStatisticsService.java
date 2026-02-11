package com.specqq.chatbot.service;

import com.specqq.chatbot.common.CQCodeConstants;
import com.specqq.chatbot.parser.CQCode;
import com.specqq.chatbot.parser.CQCodeParser;
import com.specqq.chatbot.parser.CQCodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Message Statistics Service
 *
 * <p>Calculates and formats message statistics:
 * <ul>
 *   <li>Character count (Unicode code points, excluding CQ codes)</li>
 *   <li>CQ code counts by type (face, image, at, reply, record, video)</li>
 *   <li>Formatted Chinese statistics reply (non-zero items only)</li>
 * </ul>
 * </p>
 *
 * <p>Performance Targets:
 * <ul>
 *   <li>Statistics calculation P95: <50ms</li>
 *   <li>Rate limiting: Max 1 reply per 5 seconds per group</li>
 * </ul>
 * </p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Service
public class MessageStatisticsService {

    private static final Logger logger = LoggerFactory.getLogger(MessageStatisticsService.class);

    private final CQCodeParser cqCodeParser;
    private final StringRedisTemplate redisTemplate;

    /**
     * Constructor with dependency injection
     *
     * @param cqCodeParser  CQ code parser for extracting CQ codes
     * @param redisTemplate Redis template for rate limiting
     */
    public MessageStatisticsService(CQCodeParser cqCodeParser, StringRedisTemplate redisTemplate) {
        this.cqCodeParser = cqCodeParser;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Calculate message statistics
     *
     * <p>Parses CQ codes, counts characters (Unicode code points), and groups CQ codes by type.</p>
     *
     * <p>Example:
     * <pre>
     * Input:  "Hello[CQ:face,id=1]你好[CQ:image,file=a.jpg]"
     * Output: MessageStatistics(8, {"face": 1, "image": 1})
     * </pre>
     * </p>
     *
     * @param message Message string to analyze
     * @return MessageStatistics with character count and CQ code counts
     */
    public MessageStatistics calculate(String message) {
        if (message == null || message.isEmpty()) {
            return new MessageStatistics(0, Map.of());
        }

        long startTime = System.nanoTime();

        try {
            // Parse CQ codes
            List<CQCode> cqCodes = cqCodeParser.parse(message);

            // Strip CQ codes to get plain text
            String plainText = cqCodeParser.stripCQCodes(message);

            // Count characters using Unicode code points (not String.length())
            // This correctly counts multi-byte characters like Chinese, emojis
            int characterCount = plainText.codePointCount(0, plainText.length());

            // Group CQ codes by type and count
            Map<String, Integer> cqCodeCounts = cqCodes.stream()
                    .collect(Collectors.groupingBy(
                            CQCode::type,
                            Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                    ));

            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
            if (elapsedMs > CQCodeConstants.TARGET_STATISTICS_P95_MS) {
                logger.warn("Statistics calculation exceeded P95 target: {}ms > {}ms (message length: {}, CQ codes: {})",
                        elapsedMs, CQCodeConstants.TARGET_STATISTICS_P95_MS, message.length(), cqCodes.size());
            } else {
                logger.debug("Calculated statistics in {}ms: {} chars, {} CQ codes",
                        elapsedMs, characterCount, cqCodes.size());
            }

            return new MessageStatistics(characterCount, cqCodeCounts);

        } catch (Exception e) {
            logger.error("Error calculating statistics for message: {}", message, e);
            // Return empty statistics on error to avoid crashing the pipeline
            return new MessageStatistics(0, Map.of());
        }
    }

    /**
     * Format statistics as Chinese reply message
     *
     * <p>Formats statistics with Chinese labels and units, showing ONLY non-zero items.
     * Example: "文字: 10字, 表情: 2个, 图片: 1张"</p>
     *
     * <p>Display Rules:
     * <ul>
     *   <li>Only include types with count > 0</li>
     *   <li>Text character count always shown if > 0</li>
     *   <li>CQ code types shown in standard order: face, image, at, reply, record, video</li>
     *   <li>Items joined with ", " (Chinese comma + space)</li>
     * </ul>
     * </p>
     *
     * @param statistics MessageStatistics to format
     * @return Formatted Chinese statistics string (empty if no content)
     */
    public String formatStatistics(MessageStatistics statistics) {
        if (statistics == null || statistics.isEmpty()) {
            return "";
        }

        List<String> parts = new java.util.ArrayList<>();

        // Add text character count if > 0
        if (statistics.hasText()) {
            parts.add(String.format("%s: %d%s",
                    CQCodeConstants.LABEL_TEXT,
                    statistics.characterCount(),
                    CQCodeConstants.UNIT_TEXT));
        }

        // Add CQ code counts in standard order (only non-zero)
        addCQCodeCountIfNonZero(parts, statistics, CQCodeType.FACE);
        addCQCodeCountIfNonZero(parts, statistics, CQCodeType.IMAGE);
        addCQCodeCountIfNonZero(parts, statistics, CQCodeType.AT);
        addCQCodeCountIfNonZero(parts, statistics, CQCodeType.REPLY);
        addCQCodeCountIfNonZero(parts, statistics, CQCodeType.RECORD);
        addCQCodeCountIfNonZero(parts, statistics, CQCodeType.VIDEO);

        // Add "other" types if present
        for (Map.Entry<String, Integer> entry : statistics.cqCodeCounts().entrySet()) {
            String type = entry.getKey();
            int count = entry.getValue();

            // Skip standard types (already added above) and zero counts
            if (count > 0 && CQCodeType.fromCode(type) == CQCodeType.OTHER) {
                parts.add(String.format("%s: %d%s",
                        type, count, CQCodeType.OTHER.getUnit()));
            }
        }

        return String.join(", ", parts);
    }

    /**
     * Add CQ code count to parts list if count > 0
     *
     * @param parts      List to add formatted count to
     * @param statistics Statistics containing counts
     * @param type       CQ code type to check
     */
    private void addCQCodeCountIfNonZero(List<String> parts, MessageStatistics statistics, CQCodeType type) {
        int count = statistics.getCountForType(type.getCode());
        if (count > 0) {
            parts.add(String.format("%s: %d%s", type.getLabel(), count, type.getUnit()));
        }
    }

    /**
     * Check if statistics reply is allowed (rate limiting)
     *
     * <p>Implements Redis-based rate limiting to prevent abuse.
     * Max 1 statistics reply per 5 seconds per group.</p>
     *
     * <p>Redis Key Format: {@code rate_limit:statistics:{group_id}}</p>
     * <p>Implementation: INCR + EXPIRE (atomic operation)</p>
     *
     * @param groupId Group ID to check rate limit for
     * @return true if reply is allowed, false if rate limited
     */
    public boolean isReplyAllowed(String groupId) {
        String key = CQCodeConstants.REDIS_RATE_LIMIT_PREFIX + groupId;

        try {
            // Increment counter and get current value
            Long count = redisTemplate.opsForValue().increment(key);

            if (count == null) {
                logger.warn("Redis increment returned null for key: {}", key);
                return true; // Allow on Redis error (fail open)
            }

            // Set expiration on first increment
            if (count == 1) {
                redisTemplate.expire(key, CQCodeConstants.RATE_LIMIT_WINDOW_SECONDS, TimeUnit.SECONDS);
            }

            // Check if within limit
            boolean allowed = count <= CQCodeConstants.RATE_LIMIT_MAX_REPLIES;

            if (!allowed) {
                logger.debug("Rate limit exceeded for group {}: count={}, limit={}",
                        groupId, count, CQCodeConstants.RATE_LIMIT_MAX_REPLIES);
            }

            return allowed;

        } catch (Exception e) {
            logger.error("Error checking rate limit for group {}: {}", groupId, e.getMessage());
            return true; // Allow on error (fail open to avoid blocking legitimate traffic)
        }
    }

    /**
     * Calculate and format statistics in one operation
     *
     * <p>Convenience method that combines calculate() and formatStatistics().</p>
     *
     * @param message Message string to analyze
     * @return Formatted Chinese statistics string
     */
    public String calculateAndFormat(String message) {
        MessageStatistics statistics = calculate(message);
        return formatStatistics(statistics);
    }
}
