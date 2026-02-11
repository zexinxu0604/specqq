package com.specqq.chatbot.unit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.specqq.chatbot.parser.CQCode;
import com.specqq.chatbot.parser.CQCodeParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CQCodeParser
 *
 * <p>Tests CQ code parsing functionality following TDD principles.
 * These tests should FAIL before implementation is complete.</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@DisplayName("CQCodeParser Unit Tests")
class CQCodeParserTest {

    private CQCodeParser parser;
    private Cache<String, Pattern> mockCache;

    @BeforeEach
    void setUp() {
        // Create a simple in-memory cache for testing
        mockCache = Caffeine.newBuilder()
                .maximumSize(10)
                .build();

        // Create mock Prometheus metrics
        io.micrometer.core.instrument.simple.SimpleMeterRegistry registry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        io.micrometer.core.instrument.Counter parseCounter = registry.counter("test.cqcode.parse");
        io.micrometer.core.instrument.Timer parseDurationTimer = registry.timer("test.cqcode.duration");
        io.micrometer.core.instrument.Counter cacheHitsCounter = registry.counter("test.cqcode.cache.hits");
        io.micrometer.core.instrument.Counter cacheMissesCounter = registry.counter("test.cqcode.cache.misses");
        java.util.concurrent.atomic.AtomicInteger totalCountGauge = new java.util.concurrent.atomic.AtomicInteger(0);

        parser = new CQCodeParser(mockCache, parseCounter, parseDurationTimer, cacheHitsCounter, cacheMissesCounter, totalCountGauge);
    }

    @Test
    @DisplayName("T017: should_ParseMixedMessage_When_ContainsTextAndCQCodes")
    void should_ParseMixedMessage_When_ContainsTextAndCQCodes() {
        // Given: A message with mixed text and CQ codes
        String message = "Hello[CQ:face,id=123]你好世界[CQ:image,file=abc.jpg,url=https://example.com/abc.jpg]";

        // When: Parsing the message
        List<CQCode> cqCodes = parser.parse(message);

        // Then: Should extract 2 CQ codes with correct types and parameters
        assertThat(cqCodes).hasSize(2);

        // First CQ code: face
        CQCode faceCQ = cqCodes.get(0);
        assertThat(faceCQ.type()).isEqualTo("face");
        assertThat(faceCQ.getParam("id")).isEqualTo("123");
        assertThat(faceCQ.rawText()).isEqualTo("[CQ:face,id=123]");

        // Second CQ code: image
        CQCode imageCQ = cqCodes.get(1);
        assertThat(imageCQ.type()).isEqualTo("image");
        assertThat(imageCQ.getParam("file")).isEqualTo("abc.jpg");
        assertThat(imageCQ.getParam("url")).isEqualTo("https://example.com/abc.jpg");
        assertThat(imageCQ.rawText()).isEqualTo("[CQ:image,file=abc.jpg,url=https://example.com/abc.jpg]");
    }

    @Test
    @DisplayName("T018: should_StripCQCodes_When_MessageContainsCQCodes")
    void should_StripCQCodes_When_MessageContainsCQCodes() {
        // Given: A message with CQ codes
        String message = "Hello[CQ:face,id=123]你好世界[CQ:image,file=abc.jpg]";

        // When: Stripping CQ codes
        String plainText = parser.stripCQCodes(message);

        // Then: Should return plain text without CQ codes
        assertThat(plainText).isEqualTo("Hello你好世界");
        assertThat(plainText).doesNotContain("[CQ:");
        assertThat(plainText).doesNotContain("]");
    }

    @Test
    @DisplayName("T019: should_HandleMalformedCQCode_When_MissingClosingBracket")
    void should_HandleMalformedCQCode_When_MissingClosingBracket() {
        // Given: A message with malformed CQ code (missing closing bracket)
        String message = "Test[CQ:face,id=123 message continues";

        // When: Parsing the message
        List<CQCode> cqCodes = parser.parse(message);

        // Then: Should return empty list (malformed CQ code not matched)
        assertThat(cqCodes).isEmpty();

        // When: Stripping CQ codes from malformed message
        String plainText = parser.stripCQCodes(message);

        // Then: Should return original message unchanged (nothing to strip)
        assertThat(plainText).isEqualTo(message);
    }

    @Test
    @DisplayName("should_ParseCQCodeWithoutParameters_When_NoParamsProvided")
    void should_ParseCQCodeWithoutParameters_When_NoParamsProvided() {
        // Given: A CQ code without parameters
        String message = "Test[CQ:face]message";

        // When: Parsing the message
        List<CQCode> cqCodes = parser.parse(message);

        // Then: Should extract CQ code with empty params
        assertThat(cqCodes).hasSize(1);
        CQCode cqCode = cqCodes.get(0);
        assertThat(cqCode.type()).isEqualTo("face");
        assertThat(cqCode.params()).isEmpty();
        assertThat(cqCode.rawText()).isEqualTo("[CQ:face]");
    }

    @Test
    @DisplayName("should_ParseMultipleCQCodes_When_RepeatedTypes")
    void should_ParseMultipleCQCodes_When_RepeatedTypes() {
        // Given: A message with multiple CQ codes of the same type
        String message = "[CQ:face,id=1][CQ:face,id=2][CQ:face,id=3]";

        // When: Parsing the message
        List<CQCode> cqCodes = parser.parse(message);

        // Then: Should extract all CQ codes
        assertThat(cqCodes).hasSize(3);
        assertThat(cqCodes).allMatch(cq -> cq.type().equals("face"));
        assertThat(cqCodes.get(0).getParam("id")).isEqualTo("1");
        assertThat(cqCodes.get(1).getParam("id")).isEqualTo("2");
        assertThat(cqCodes.get(2).getParam("id")).isEqualTo("3");
    }

    @Test
    @DisplayName("should_ReturnEmptyList_When_MessageIsEmpty")
    void should_ReturnEmptyList_When_MessageIsEmpty() {
        // Given: An empty message
        String message = "";

        // When: Parsing the message
        List<CQCode> cqCodes = parser.parse(message);

        // Then: Should return empty list
        assertThat(cqCodes).isEmpty();
    }

    @Test
    @DisplayName("should_ReturnEmptyList_When_MessageIsNull")
    void should_ReturnEmptyList_When_MessageIsNull() {
        // Given: A null message
        String message = null;

        // When: Parsing the message
        List<CQCode> cqCodes = parser.parse(message);

        // Then: Should return empty list (no exception)
        assertThat(cqCodes).isEmpty();
    }

    @Test
    @DisplayName("should_ParseUnknownCQCodeType_When_NotStandardType")
    void should_ParseUnknownCQCodeType_When_NotStandardType() {
        // Given: A message with unknown CQ code type
        String message = "Test[CQ:future_type,param=value]message";

        // When: Parsing the message
        List<CQCode> cqCodes = parser.parse(message);

        // Then: Should extract CQ code with unknown type
        assertThat(cqCodes).hasSize(1);
        CQCode cqCode = cqCodes.get(0);
        assertThat(cqCode.type()).isEqualTo("future_type");
        assertThat(cqCode.getParam("param")).isEqualTo("value");
    }

    @Test
    @DisplayName("should_ValidateCQCodeSyntax_When_WellFormed")
    void should_ValidateCQCodeSyntax_When_WellFormed() {
        // Given: A well-formed CQ code
        String cqCode = "[CQ:face,id=123]";

        // When: Validating the CQ code
        CQCodeParser.ValidationResult result = parser.validate(cqCode);

        // Then: Should return valid result
        assertThat(result.isValid()).isTrue();
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    @DisplayName("should_RejectInvalidSyntax_When_MissingPrefix")
    void should_RejectInvalidSyntax_When_MissingPrefix() {
        // Given: A CQ code missing [CQ: prefix
        String cqCode = "face,id=123]";

        // When: Validating the CQ code
        CQCodeParser.ValidationResult result = parser.validate(cqCode);

        // Then: Should return invalid result
        assertThat(result.isValid()).isFalse();
        assertThat(result.errorMessage()).isNotBlank();
    }

    @Test
    @DisplayName("should_StripMultipleCQCodes_When_MessageHasMany")
    void should_StripMultipleCQCodes_When_MessageHasMany() {
        // Given: A message with multiple CQ codes
        String message = "Start[CQ:face,id=1]Middle[CQ:image,file=a.jpg]End[CQ:at,qq=123]";

        // When: Stripping CQ codes
        String plainText = parser.stripCQCodes(message);

        // Then: Should remove all CQ codes
        assertThat(plainText).isEqualTo("StartMiddleEnd");
    }

    @Test
    @DisplayName("should_HandleLongMessage_When_ManyCQCodes")
    void should_HandleLongMessage_When_ManyCQCodes() {
        // Given: A message with 50 CQ codes (performance test)
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 1; i <= 50; i++) {
            messageBuilder.append("[CQ:face,id=").append(i).append("]");
        }
        String message = messageBuilder.toString();

        // When: Parsing the message
        long startTime = System.nanoTime();
        List<CQCode> cqCodes = parser.parse(message);
        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;

        // Then: Should extract all 50 CQ codes within performance target
        assertThat(cqCodes).hasSize(50);
        assertThat(elapsedMs).isLessThan(10); // P95 target: <10ms
    }
}
