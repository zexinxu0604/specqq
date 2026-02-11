package com.specqq.chatbot.parser;

import com.github.benmanes.caffeine.cache.Cache;
import com.specqq.chatbot.common.CQCodeConstants;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CQ Code Parser
 *
 * <p>Parses CQ codes from OneBot 11 protocol message strings.
 * Uses regex-based parsing with compiled pattern caching for performance.</p>
 *
 * <p>Performance Characteristics:
 * <ul>
 *   <li>P95 latency: <10ms (target)</li>
 *   <li>Cache hit rate: ≥95% (target)</li>
 *   <li>Handles up to 50 CQ codes per message efficiently</li>
 *   <li>Gracefully handles malformed CQ codes (treats as plain text)</li>
 * </ul>
 * </p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Component
public class CQCodeParser {

    private static final Logger logger = LoggerFactory.getLogger(CQCodeParser.class);

    private final Cache<String, Pattern> patternCache;
    private final Pattern cqCodePattern;

    // T117: Prometheus metrics
    private final Counter parseCounter;
    private final Timer parseDurationTimer;
    private final Counter cacheHitsCounter;
    private final Counter cacheMissesCounter;
    private final AtomicInteger totalCountGauge;

    /**
     * Constructor with dependency injection
     *
     * @param patternCache Caffeine cache for compiled patterns
     * @param parseCounter Counter for parse operations
     * @param parseDurationTimer Timer for parse duration
     * @param cacheHitsCounter Counter for cache hits
     * @param cacheMissesCounter Counter for cache misses
     * @param totalCountGauge Gauge for total CQ codes parsed
     */
    public CQCodeParser(@Qualifier(CQCodeConstants.CACHE_CQ_PATTERNS) Cache<String, Pattern> patternCache,
                        @Qualifier("cqCodeParseCounter") Counter parseCounter,
                        @Qualifier("cqCodeParseDurationTimer") Timer parseDurationTimer,
                        @Qualifier("cqCodeCacheHitsCounter") Counter cacheHitsCounter,
                        @Qualifier("cqCodeCacheMissesCounter") Counter cacheMissesCounter,
                        @Qualifier("cqCodeTotalCount") AtomicInteger totalCountGauge) {
        this.patternCache = patternCache;
        this.cqCodePattern = Pattern.compile(CQCodeConstants.CQ_CODE_PATTERN);
        this.parseCounter = parseCounter;
        this.parseDurationTimer = parseDurationTimer;
        this.cacheHitsCounter = cacheHitsCounter;
        this.cacheMissesCounter = cacheMissesCounter;
        this.totalCountGauge = totalCountGauge;
        logger.info("CQCodeParser initialized with pattern: {}", CQCodeConstants.CQ_CODE_PATTERN);
    }

    /**
     * Parse CQ codes from message string
     *
     * <p>Extracts all CQ codes from the message and returns them as a list.
     * Malformed CQ codes are logged but do not throw exceptions.</p>
     *
     * <p>Example:
     * <pre>
     * Input:  "Hello[CQ:face,id=123]你好[CQ:image,file=abc.jpg]"
     * Output: [CQCode("face", {"id":"123"}, "[CQ:face,id=123]"),
     *          CQCode("image", {"file":"abc.jpg"}, "[CQ:image,file=abc.jpg]")]
     * </pre>
     * </p>
     *
     * @param message Message string to parse (may contain CQ codes)
     * @return List of parsed CQ codes (empty list if none found)
     */
    public List<CQCode> parse(String message) {
        if (message == null || message.isEmpty()) {
            return Collections.emptyList();
        }

        // T117: Record parse operation
        parseCounter.increment();
        long startTime = System.nanoTime();
        List<CQCode> cqCodes = new ArrayList<>();

        try {
            Matcher matcher = cqCodePattern.matcher(message);

            while (matcher.find()) {
                String rawText = matcher.group(0);
                String type = matcher.group(1);
                String paramsString = matcher.group(2);

                Map<String, String> params = parseParams(paramsString);
                CQCode cqCode = new CQCode(type, params, rawText);
                cqCodes.add(cqCode);
            }

            // T117: Update total CQ code count gauge
            totalCountGauge.addAndGet(cqCodes.size());

            // T117: Record parse duration
            long elapsedNanos = System.nanoTime() - startTime;
            parseDurationTimer.record(elapsedNanos, java.util.concurrent.TimeUnit.NANOSECONDS);

            long elapsedMs = elapsedNanos / 1_000_000;
            if (elapsedMs > CQCodeConstants.TARGET_PARSE_P95_MS) {
                logger.warn("CQ code parsing exceeded P95 target: {}ms > {}ms (message length: {}, CQ codes: {})",
                        elapsedMs, CQCodeConstants.TARGET_PARSE_P95_MS, message.length(), cqCodes.size());
            } else {
                logger.debug("Parsed {} CQ codes from message in {}ms", cqCodes.size(), elapsedMs);
            }

        } catch (Exception e) {
            logger.error("Error parsing CQ codes from message: {}", message, e);
            // Return empty list on error to avoid crashing the pipeline
        }

        return cqCodes;
    }

    /**
     * Strip CQ codes from message and return plain text
     *
     * <p>Removes all CQ codes from the message, leaving only plain text.
     * Useful for character counting and text analysis.</p>
     *
     * <p>Example:
     * <pre>
     * Input:  "Hello[CQ:face,id=123]你好[CQ:image,file=abc.jpg]"
     * Output: "Hello你好"
     * </pre>
     * </p>
     *
     * @param message Message string (may contain CQ codes)
     * @return Plain text with CQ codes removed
     */
    public String stripCQCodes(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        try {
            return cqCodePattern.matcher(message).replaceAll("");
        } catch (Exception e) {
            logger.error("Error stripping CQ codes from message: {}", message, e);
            return message; // Return original message on error
        }
    }

    /**
     * Validate CQ code syntax
     *
     * <p>Checks if a CQ code string has valid syntax according to OneBot 11 specification.
     * Does not validate parameter values, only syntax structure.</p>
     *
     * @param cqCode CQ code string to validate (e.g., "[CQ:face,id=123]")
     * @return ValidationResult with success flag and error message if invalid
     */
    public ValidationResult validate(String cqCode) {
        if (cqCode == null || cqCode.isEmpty()) {
            return new ValidationResult(false, "CQ code cannot be empty");
        }

        if (!cqCode.startsWith("[CQ:") || !cqCode.endsWith("]")) {
            return new ValidationResult(false, "CQ code must start with '[CQ:' and end with ']'");
        }

        Matcher matcher = cqCodePattern.matcher(cqCode);
        if (!matcher.matches()) {
            return new ValidationResult(false, "Invalid CQ code syntax");
        }

        String type = matcher.group(1);
        if (type == null || type.isBlank()) {
            return new ValidationResult(false, "CQ code type cannot be empty");
        }

        return new ValidationResult(true, null);
    }

    /**
     * Parse parameters from CQ code parameter string
     *
     * <p>Parses comma-separated key=value pairs into a map.
     * Example: "id=123,url=https://example.com" → {"id": "123", "url": "https://example.com"}</p>
     *
     * @param paramsString Parameter string from CQ code (may be null)
     * @return Map of parameter key-value pairs (empty map if no params)
     */
    private Map<String, String> parseParams(String paramsString) {
        if (paramsString == null || paramsString.isBlank()) {
            return Collections.emptyMap();
        }

        Map<String, String> params = new HashMap<>();
        String[] pairs = paramsString.split(",");

        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                if (!key.isEmpty()) {
                    params.put(key, value);
                }
            } else {
                logger.warn("Malformed parameter pair in CQ code: {}", pair);
            }
        }

        return params;
    }

    /**
     * Validation Result
     *
     * @param valid        Whether the CQ code is valid
     * @param errorMessage Error message if invalid (null if valid)
     */
    public record ValidationResult(boolean valid, String errorMessage) {
        public boolean isValid() {
            return valid;
        }
    }
}
