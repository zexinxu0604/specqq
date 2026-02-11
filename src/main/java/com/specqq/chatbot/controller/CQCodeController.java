package com.specqq.chatbot.controller;

import com.specqq.chatbot.common.Result;
import com.specqq.chatbot.dto.ParseCQCodeRequestDTO;
import com.specqq.chatbot.dto.StripCQCodeRequestDTO;
import com.specqq.chatbot.dto.ValidateCQCodeRequestDTO;
import com.specqq.chatbot.dto.CQCodePatternDTO;
import com.specqq.chatbot.parser.CQCode;
import com.specqq.chatbot.parser.CQCodeParser;
import com.specqq.chatbot.parser.CQCodeType;
import com.specqq.chatbot.service.CQCodeService;
import com.specqq.chatbot.service.MessageStatistics;
import com.specqq.chatbot.service.MessageStatisticsService;
import com.specqq.chatbot.vo.CQCodeVO;
import com.specqq.chatbot.vo.MessageStatisticsVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CQ Code API Controller
 *
 * <p>Provides RESTful endpoints for CQ code parsing, validation, and statistics calculation.</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Slf4j
@RestController
@RequestMapping("/api/cqcode")
@RequiredArgsConstructor
public class CQCodeController {

    private final CQCodeParser cqCodeParser;
    private final MessageStatisticsService statisticsService;
    private final CQCodeService cqCodeService;

    /**
     * Parse CQ codes from message string
     *
     * <p>POST /api/cqcode/parse</p>
     *
     * @param request Parse request with message content
     * @return List of parsed CQ codes
     */
    @PostMapping("/parse")
    public Result<List<CQCodeVO>> parseCQCodes(@Valid @RequestBody ParseCQCodeRequestDTO request) {
        try {
            List<CQCode> cqCodes = cqCodeParser.parse(request.getMessage());

            List<CQCodeVO> voList = cqCodes.stream()
                    .map(this::toCQCodeVO)
                    .collect(Collectors.toList());

            return Result.success(voList);
        } catch (Exception e) {
            log.error("Failed to parse CQ codes: message={}", request.getMessage(), e);
            return Result.error("Failed to parse CQ codes: " + e.getMessage());
        }
    }

    /**
     * Strip CQ codes from message and return character count
     *
     * <p>POST /api/cqcode/strip</p>
     *
     * @param request Strip request with message content
     * @return Plain text and character count
     */
    @PostMapping("/strip")
    public Result<Map<String, Object>> stripCQCodes(@Valid @RequestBody StripCQCodeRequestDTO request) {
        try {
            String plainText = cqCodeParser.stripCQCodes(request.getMessage());
            int characterCount = plainText.codePointCount(0, plainText.length());

            Map<String, Object> result = new HashMap<>();
            result.put("plainText", plainText);
            result.put("characterCount", characterCount);

            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to strip CQ codes: message={}", request.getMessage(), e);
            return Result.error("Failed to strip CQ codes: " + e.getMessage());
        }
    }

    /**
     * Validate CQ code syntax
     *
     * <p>POST /api/cqcode/validate</p>
     *
     * @param request Validate request with CQ code string
     * @return Validation result
     */
    @PostMapping("/validate")
    public Result<Map<String, Object>> validateCQCode(@Valid @RequestBody ValidateCQCodeRequestDTO request) {
        try {
            CQCodeParser.ValidationResult validation = cqCodeParser.validate(request.getCqCode());

            Map<String, Object> result = new HashMap<>();
            result.put("valid", validation.isValid());
            result.put("errorMessage", validation.errorMessage());

            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to validate CQ code: cqCode={}", request.getCqCode(), e);
            return Result.error("Failed to validate CQ code: " + e.getMessage());
        }
    }

    /**
     * Get supported CQ code types with Chinese labels
     *
     * <p>GET /api/cqcode/types</p>
     *
     * @return List of CQ code types
     */
    @GetMapping("/types")
    public Result<List<Map<String, String>>> getCQCodeTypes() {
        try {
            List<Map<String, String>> types = Arrays.stream(CQCodeType.values())
                    .filter(CQCodeType::isStandardType) // Exclude OTHER
                    .map(type -> {
                        Map<String, String> typeInfo = new HashMap<>();
                        typeInfo.put("code", type.getCode());
                        typeInfo.put("label", type.getLabel());
                        typeInfo.put("unit", type.getUnit());
                        return typeInfo;
                    })
                    .collect(Collectors.toList());

            return Result.success(types);
        } catch (Exception e) {
            log.error("Failed to get CQ code types", e);
            return Result.error("Failed to get CQ code types: " + e.getMessage());
        }
    }

    /**
     * Convert CQCode to CQCodeVO
     *
     * @param cqCode CQCode entity
     * @return CQCodeVO view object
     */
    private CQCodeVO toCQCodeVO(CQCode cqCode) {
        CQCodeVO vo = new CQCodeVO();
        vo.setType(cqCode.type());
        vo.setParams(cqCode.params());
        vo.setRawText(cqCode.rawText());

        // Add Chinese label
        CQCodeType cqCodeType = CQCodeType.fromCode(cqCode.type());
        vo.setLabel(cqCodeType.getLabel());
        vo.setUnit(cqCodeType.getUnit());

        return vo;
    }

    /**
     * Get predefined CQ code patterns for UI dropdown
     *
     * <p>GET /api/cqcode/patterns</p>
     *
     * @return List of predefined patterns
     */
    @GetMapping("/patterns")
    public Result<List<CQCodePatternDTO>> getPredefinedPatterns() {
        try {
            List<CQCodePatternDTO> patterns = cqCodeService.listPredefinedPatterns();
            return Result.success(patterns);
        } catch (Exception e) {
            log.error("Failed to get predefined patterns", e);
            return Result.error("Failed to get predefined patterns: " + e.getMessage());
        }
    }

    /**
     * Validate custom CQ code pattern
     *
     * <p>POST /api/cqcode/patterns/validate</p>
     *
     * @param request Request with regex pattern to validate
     * @return Validation result
     */
    @PostMapping("/patterns/validate")
    public Result<Map<String, Object>> validatePattern(@Valid @RequestBody ValidateCQCodeRequestDTO request) {
        try {
            boolean isValid = cqCodeService.validatePattern(request.getCqCode());

            Map<String, Object> result = new HashMap<>();
            result.put("valid", isValid);
            if (!isValid) {
                result.put("errorMessage", "Invalid regex pattern syntax");
            }

            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to validate pattern: pattern={}", request.getCqCode(), e);
            return Result.error("Failed to validate pattern: " + e.getMessage());
        }
    }
}
