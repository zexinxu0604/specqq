package com.specqq.chatbot.unit;

import com.specqq.chatbot.service.CQCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for CQCodeService
 *
 * <p>Tests CQ code pattern generation and validation following TDD principles.
 * These tests should FAIL before implementation is complete.</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CQ Code Service Tests")
class CQCodeServiceTest {

    private CQCodeService service;

    @BeforeEach
    void setUp() {
        service = new CQCodeService();
    }

    /**
     * T057: Test regex pattern generation for specific CQ code types
     */
    @Test
    @DisplayName("should_GenerateRegexPattern_When_CQCodeTypeSelected")
    void should_GenerateRegexPattern_When_CQCodeTypeSelected() {
        // Given: CQ code type "image"
        String cqCodeType = "image";

        // When: Generating regex pattern
        String pattern = service.generateRegexPattern(cqCodeType);

        // Then: Should return pattern matching [CQ:image,...]
        assertThat(pattern).isNotNull();
        assertThat(pattern).contains("\\[CQ:image");
        assertThat(pattern).matches(".*\\\\\\[CQ:image.*"); // Escaped bracket pattern
    }

    @Test
    @DisplayName("should_GenerateRegexPattern_When_FaceTypeSelected")
    void should_GenerateRegexPattern_When_FaceTypeSelected() {
        // Given: CQ code type "face"
        String cqCodeType = "face";

        // When: Generating regex pattern
        String pattern = service.generateRegexPattern(cqCodeType);

        // Then: Should return pattern matching [CQ:face,...]
        assertThat(pattern).isNotNull();
        assertThat(pattern).contains("\\[CQ:face");
    }

    @Test
    @DisplayName("should_GenerateRegexPattern_When_AtTypeSelected")
    void should_GenerateRegexPattern_When_AtTypeSelected() {
        // Given: CQ code type "at"
        String cqCodeType = "at";

        // When: Generating regex pattern
        String pattern = service.generateRegexPattern(cqCodeType);

        // Then: Should return pattern matching [CQ:at,...]
        assertThat(pattern).isNotNull();
        assertThat(pattern).contains("\\[CQ:at");
    }

    /**
     * T058: Test validation of custom regex patterns
     */
    @Test
    @DisplayName("should_ValidatePattern_When_CustomRegexProvided")
    void should_ValidatePattern_When_CustomRegexProvided() {
        // Given: Valid custom regex pattern
        String validPattern = "\\[CQ:image,file=.*\\.jpg\\]";

        // When: Validating pattern
        boolean isValid = service.validatePattern(validPattern);

        // Then: Should return true
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("should_RejectPattern_When_InvalidRegexProvided")
    void should_RejectPattern_When_InvalidRegexProvided() {
        // Given: Invalid regex pattern (unmatched bracket)
        String invalidPattern = "[CQ:image,file=.*\\.jpg";

        // When: Validating pattern
        boolean isValid = service.validatePattern(invalidPattern);

        // Then: Should return false
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("should_RejectPattern_When_NullPatternProvided")
    void should_RejectPattern_When_NullPatternProvided() {
        // Given: Null pattern
        String nullPattern = null;

        // When: Validating pattern
        boolean isValid = service.validatePattern(nullPattern);

        // Then: Should return false
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("should_RejectPattern_When_EmptyPatternProvided")
    void should_RejectPattern_When_EmptyPatternProvided() {
        // Given: Empty pattern
        String emptyPattern = "";

        // When: Validating pattern
        boolean isValid = service.validatePattern(emptyPattern);

        // Then: Should return false
        assertThat(isValid).isFalse();
    }

    /**
     * T059: Test pattern combination with logical operators
     */
    @Test
    @DisplayName("should_CombinePatterns_When_LogicalOperatorProvided")
    void should_CombinePatterns_When_LogicalOperatorProvided() {
        // Given: Two patterns and AND operator
        String pattern1 = "\\[CQ:image";
        String pattern2 = "\\[CQ:face";
        String operator = "AND";

        // When: Combining patterns
        String combined = service.combinePatterns(pattern1, pattern2, operator);

        // Then: Should return combined pattern with both conditions
        assertThat(combined).isNotNull();
        assertThat(combined).contains(pattern1);
        assertThat(combined).contains(pattern2);
    }

    @Test
    @DisplayName("should_CombinePatterns_When_OROperatorProvided")
    void should_CombinePatterns_When_OROperatorProvided() {
        // Given: Two patterns and OR operator
        String pattern1 = "\\[CQ:image";
        String pattern2 = "\\[CQ:face";
        String operator = "OR";

        // When: Combining patterns
        String combined = service.combinePatterns(pattern1, pattern2, operator);

        // Then: Should return combined pattern with either condition
        assertThat(combined).isNotNull();
        assertThat(combined).contains(pattern1);
        assertThat(combined).contains(pattern2);
        assertThat(combined).contains("|"); // OR operator in regex
    }

    @Test
    @DisplayName("should_ThrowException_When_InvalidOperatorProvided")
    void should_ThrowException_When_InvalidOperatorProvided() {
        // Given: Two patterns and invalid operator
        String pattern1 = "\\[CQ:image";
        String pattern2 = "\\[CQ:face";
        String invalidOperator = "XOR";

        // When/Then: Should throw exception
        assertThatThrownBy(() -> service.combinePatterns(pattern1, pattern2, invalidOperator))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid operator");
    }

    @Test
    @DisplayName("should_GeneratePatternWithParamFilter_When_ParamSpecified")
    void should_GeneratePatternWithParamFilter_When_ParamSpecified() {
        // Given: CQ code type with parameter filter
        String cqCodeType = "at";
        String paramFilter = "qq=123456";

        // When: Generating pattern with param filter
        String pattern = service.generateRegexPatternWithParam(cqCodeType, paramFilter);

        // Then: Should include param in pattern
        assertThat(pattern).isNotNull();
        assertThat(pattern).contains("\\[CQ:at");
        assertThat(pattern).contains("qq=123456");
    }

    @Test
    @DisplayName("should_ListPredefinedPatterns_When_Requested")
    void should_ListPredefinedPatterns_When_Requested() {
        // When: Listing predefined patterns
        var patterns = service.listPredefinedPatterns();

        // Then: Should return common patterns
        assertThat(patterns).isNotEmpty();
        assertThat(patterns).hasSize(6); // face, image, at, reply, record, video
        assertThat(patterns.get(0).getType()).isEqualTo("face");
        assertThat(patterns.get(0).getLabel()).isEqualTo("表情");
    }
}
