package com.specqq.chatbot.service;

import com.specqq.chatbot.common.CQCodeConstants;
import com.specqq.chatbot.dto.CQCodePatternDTO;
import com.specqq.chatbot.parser.CQCodeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * CQ Code Service
 *
 * <p>Provides CQ code pattern generation, validation, and combination services.</p>
 *
 * <p>Features:
 * <ul>
 *   <li>Generate regex patterns for CQ code types (image, face, at, etc.)</li>
 *   <li>Validate custom regex patterns</li>
 *   <li>Combine patterns with logical operators (AND, OR)</li>
 *   <li>Apply parameter filters (e.g., qq=123456)</li>
 *   <li>List predefined patterns for UI dropdown</li>
 * </ul>
 * </p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CQCodeService {

    /**
     * Generate regex pattern for a specific CQ code type
     *
     * <p>Example:
     * <pre>
     * generateRegexPattern("image") → "\\[CQ:image(?:,[^\\]]+)?\\]"
     * </pre>
     * </p>
     *
     * @param cqCodeType CQ code type (face, image, at, reply, record, video)
     * @return Regex pattern matching the CQ code type
     */
    public String generateRegexPattern(String cqCodeType) {
        if (cqCodeType == null || cqCodeType.trim().isEmpty()) {
            throw new IllegalArgumentException("CQ code type cannot be null or empty");
        }

        // Validate CQ code type
        CQCodeType type = CQCodeType.fromCode(cqCodeType);
        if (type == CQCodeType.OTHER) {
            log.warn("Unknown CQ code type: {}, generating generic pattern", cqCodeType);
        }

        // Generate pattern: \[CQ:type(?:,[^\]]+)?\]
        // Matches: [CQ:type] or [CQ:type,param1=value1,param2=value2]
        return String.format("\\[CQ:%s(?:,[^\\]]+)?\\]", cqCodeType);
    }

    /**
     * Generate regex pattern with parameter filter
     *
     * <p>Example:
     * <pre>
     * generateRegexPatternWithParam("at", "qq=123456")
     *   → "\\[CQ:at,.*qq=123456.*\\]"
     * </pre>
     * </p>
     *
     * @param cqCodeType  CQ code type
     * @param paramFilter Parameter filter (e.g., "qq=123456")
     * @return Regex pattern with parameter filter
     */
    public String generateRegexPatternWithParam(String cqCodeType, String paramFilter) {
        if (cqCodeType == null || cqCodeType.trim().isEmpty()) {
            throw new IllegalArgumentException("CQ code type cannot be null or empty");
        }
        if (paramFilter == null || paramFilter.trim().isEmpty()) {
            return generateRegexPattern(cqCodeType);
        }

        // Generate pattern: \[CQ:type,.*param=value.*\]
        return String.format("\\[CQ:%s,.*%s.*\\]", cqCodeType, Pattern.quote(paramFilter));
    }

    /**
     * Validate regex pattern syntax
     *
     * @param pattern Regex pattern to validate
     * @return true if valid, false otherwise
     */
    public boolean validatePattern(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return false;
        }

        try {
            Pattern.compile(pattern);
            return true;
        } catch (PatternSyntaxException e) {
            log.warn("Invalid regex pattern: {}, error: {}", pattern, e.getMessage());
            return false;
        }
    }

    /**
     * Combine two patterns with logical operator
     *
     * <p>Supported operators:
     * <ul>
     *   <li>AND: Both patterns must match (pattern1 AND pattern2)</li>
     *   <li>OR: Either pattern must match (pattern1 OR pattern2)</li>
     * </ul>
     * </p>
     *
     * <p>Example:
     * <pre>
     * combinePatterns("\\[CQ:image", "\\[CQ:face", "OR")
     *   → "(\\[CQ:image|\\[CQ:face)"
     * </pre>
     * </p>
     *
     * @param pattern1 First pattern
     * @param pattern2 Second pattern
     * @param operator Logical operator (AND, OR)
     * @return Combined pattern
     */
    public String combinePatterns(String pattern1, String pattern2, String operator) {
        if (pattern1 == null || pattern1.trim().isEmpty()) {
            throw new IllegalArgumentException("Pattern1 cannot be null or empty");
        }
        if (pattern2 == null || pattern2.trim().isEmpty()) {
            throw new IllegalArgumentException("Pattern2 cannot be null or empty");
        }
        if (operator == null || operator.trim().isEmpty()) {
            throw new IllegalArgumentException("Operator cannot be null or empty");
        }

        return switch (operator.toUpperCase()) {
            case "AND" -> {
                // AND: Message must contain both patterns
                // Use positive lookahead for both conditions
                yield String.format("(?=.*%s)(?=.*%s)", pattern1, pattern2);
            }
            case "OR" -> {
                // OR: Message must contain either pattern
                // Use alternation
                yield String.format("(%s|%s)", pattern1, pattern2);
            }
            default -> throw new IllegalArgumentException("Invalid operator: " + operator + ". Supported: AND, OR");
        };
    }

    /**
     * List predefined CQ code patterns for UI dropdown
     *
     * <p>Returns standard CQ code types with Chinese labels and regex patterns.</p>
     *
     * @return List of predefined patterns
     */
    public List<CQCodePatternDTO> listPredefinedPatterns() {
        List<CQCodePatternDTO> patterns = new ArrayList<>();

        // Add standard CQ code types
        patterns.add(new CQCodePatternDTO(
                "face",
                CQCodeConstants.LABEL_FACE,
                generateRegexPattern("face")
        ));

        patterns.add(new CQCodePatternDTO(
                "image",
                CQCodeConstants.LABEL_IMAGE,
                generateRegexPattern("image")
        ));

        patterns.add(new CQCodePatternDTO(
                "at",
                CQCodeConstants.LABEL_AT,
                generateRegexPattern("at")
        ));

        patterns.add(new CQCodePatternDTO(
                "reply",
                CQCodeConstants.LABEL_REPLY,
                generateRegexPattern("reply")
        ));

        patterns.add(new CQCodePatternDTO(
                "record",
                CQCodeConstants.LABEL_RECORD,
                generateRegexPattern("record")
        ));

        patterns.add(new CQCodePatternDTO(
                "video",
                CQCodeConstants.LABEL_VIDEO,
                generateRegexPattern("video")
        ));

        return patterns;
    }

    /**
     * Get CQ code type label
     *
     * @param cqCodeType CQ code type
     * @return Chinese label
     */
    public String getTypeLabel(String cqCodeType) {
        CQCodeType type = CQCodeType.fromCode(cqCodeType);
        return type.getLabel();
    }

    /**
     * Get CQ code type unit
     *
     * @param cqCodeType CQ code type
     * @return Chinese unit
     */
    public String getTypeUnit(String cqCodeType) {
        CQCodeType type = CQCodeType.fromCode(cqCodeType);
        return type.getUnit();
    }
}
