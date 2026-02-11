package com.specqq.chatbot.controller;

import com.specqq.chatbot.common.RateLimit;
import com.specqq.chatbot.common.Result;
import com.specqq.chatbot.dto.CalculateStatisticsRequestDTO;
import com.specqq.chatbot.dto.FormatStatisticsRequestDTO;
import com.specqq.chatbot.dto.TestStatisticsRequestDTO;
import com.specqq.chatbot.service.MessageStatistics;
import com.specqq.chatbot.service.MessageStatisticsService;
import com.specqq.chatbot.vo.MessageStatisticsVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Message Statistics API Controller
 *
 * <p>Provides RESTful endpoints for message statistics calculation and formatting.</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Slf4j
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final MessageStatisticsService statisticsService;

    /**
     * Calculate message statistics
     *
     * <p>POST /api/statistics/calculate</p>
     *
     * <p>Rate limit: 100 requests per minute per IP (T116)</p>
     *
     * @param request Calculate request with message content
     * @return Message statistics
     */
    @PostMapping("/calculate")
    @RateLimit(limit = 100, windowSeconds = 60)
    public Result<MessageStatisticsVO> calculateStatistics(@Valid @RequestBody CalculateStatisticsRequestDTO request) {
        try {
            MessageStatistics statistics = statisticsService.calculate(request.getMessage());
            MessageStatisticsVO vo = toStatisticsVO(statistics);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("Failed to calculate statistics: message={}", request.getMessage(), e);
            return Result.error("Failed to calculate statistics: " + e.getMessage());
        }
    }

    /**
     * Format statistics as Chinese reply
     *
     * <p>POST /api/statistics/format</p>
     *
     * <p>Rate limit: 100 requests per minute per IP (T116)</p>
     *
     * @param request Format request with statistics
     * @return Formatted Chinese statistics string
     */
    @PostMapping("/format")
    @RateLimit(limit = 100, windowSeconds = 60)
    public Result<Map<String, String>> formatStatistics(@Valid @RequestBody FormatStatisticsRequestDTO request) {
        try {
            MessageStatistics statistics = new MessageStatistics(
                    request.getCharacterCount(),
                    request.getCqCodeCounts()
            );

            String formatted = statisticsService.formatStatistics(statistics);

            Map<String, String> result = new HashMap<>();
            result.put("formatted", formatted);

            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to format statistics", e);
            return Result.error("Failed to format statistics: " + e.getMessage());
        }
    }

    /**
     * Calculate and format statistics in one operation
     *
     * <p>POST /api/statistics/calculate-and-format</p>
     *
     * <p>Rate limit: 100 requests per minute per IP (T116)</p>
     *
     * @param request Calculate request with message content
     * @return Formatted Chinese statistics string
     */
    @PostMapping("/calculate-and-format")
    @RateLimit(limit = 100, windowSeconds = 60)
    public Result<Map<String, String>> calculateAndFormat(@Valid @RequestBody CalculateStatisticsRequestDTO request) {
        try {
            String formatted = statisticsService.calculateAndFormat(request.getMessage());

            Map<String, String> result = new HashMap<>();
            result.put("formatted", formatted);

            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to calculate and format statistics: message={}", request.getMessage(), e);
            return Result.error("Failed to calculate and format statistics: " + e.getMessage());
        }
    }

    /**
     * Test statistics with debug info
     *
     * <p>POST /api/statistics/test</p>
     *
     * <p>Rate limit: 100 requests per minute per IP (T116)</p>
     *
     * @param request Test request with message content
     * @return Statistics with debug info (execution time, cache hit)
     */
    @PostMapping("/test")
    @RateLimit(limit = 100, windowSeconds = 60)
    public Result<Map<String, Object>> testStatistics(@Valid @RequestBody TestStatisticsRequestDTO request) {
        try {
            long startTime = System.nanoTime();

            MessageStatistics statistics = statisticsService.calculate(request.getMessage());
            String formatted = statisticsService.formatStatistics(statistics);

            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;

            Map<String, Object> result = new HashMap<>();
            result.put("statistics", toStatisticsVO(statistics));
            result.put("formatted", formatted);
            result.put("executionTimeMs", elapsedMs);
            result.put("message", request.getMessage());

            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to test statistics: message={}", request.getMessage(), e);
            return Result.error("Failed to test statistics: " + e.getMessage());
        }
    }

    /**
     * Convert MessageStatistics to MessageStatisticsVO
     *
     * @param statistics MessageStatistics entity
     * @return MessageStatisticsVO view object
     */
    private MessageStatisticsVO toStatisticsVO(MessageStatistics statistics) {
        MessageStatisticsVO vo = new MessageStatisticsVO();
        vo.setCharacterCount(statistics.characterCount());
        vo.setCqCodeCounts(statistics.cqCodeCounts());
        vo.setTotalCQCodeCount(statistics.getTotalCQCodeCount());
        vo.setHasText(statistics.hasText());
        vo.setHasCQCodes(statistics.hasCQCodes());
        return vo;
    }
}
