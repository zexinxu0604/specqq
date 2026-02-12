package com.specqq.chatbot.controller;

import com.specqq.chatbot.common.Result;
import com.specqq.chatbot.common.ResultCode;
import com.specqq.chatbot.handler.HandlerMetadata;
import com.specqq.chatbot.handler.HandlerParam;
import com.specqq.chatbot.service.HandlerRegistryService;
import com.specqq.chatbot.vo.HandlerMetadataVO;
import com.specqq.chatbot.vo.HandlerParamVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handler 管理控制器
 *
 * <p>提供 Handler 元数据查询接口</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Slf4j
@RestController
@RequestMapping("/api/handlers")
@RequiredArgsConstructor
@Validated
@Tag(name = "Handler 管理", description = "Handler 元数据查询接口")
public class HandlerController {

    private final HandlerRegistryService handlerRegistryService;

    /**
     * 获取所有 Handler 列表
     */
    @GetMapping
    @Operation(summary = "获取所有 Handler", description = "返回所有已注册的 Handler 元数据列表")
    public Result<List<HandlerMetadataVO>> listHandlers(
        @Parameter(description = "分类筛选") @RequestParam(required = false) String category
    ) {
        log.info("查询 Handler 列表: category={}", category);

        List<HandlerMetadata> metadataList = handlerRegistryService.getAllHandlerMetadata();

        // 转换为 VO
        List<HandlerMetadataVO> voList = metadataList.stream()
                .filter(metadata -> category == null || category.isEmpty() || category.equals(metadata.category()))
                .map(this::convertToVO)
                .collect(Collectors.toList());

        log.info("返回 {} 个 Handler", voList.size());

        return Result.success(voList);
    }

    /**
     * 根据类型获取 Handler 详情
     */
    @GetMapping("/{handlerType}")
    @Operation(summary = "获取 Handler 详情", description = "根据 Handler 类型获取详细信息")
    public Result<HandlerMetadataVO> getHandlerByType(
        @Parameter(description = "Handler 类型") @PathVariable String handlerType
    ) {
        log.info("查询 Handler 详情: handlerType={}", handlerType);

        HandlerMetadata metadata = handlerRegistryService.getHandlerMetadata(handlerType);

        if (metadata == null) {
            return Result.error(ResultCode.NOT_FOUND, "Handler 不存在: " + handlerType);
        }

        HandlerMetadataVO vo = convertToVO(metadata);

        return Result.success(vo);
    }

    /**
     * 获取所有 Handler 类型列表
     */
    @GetMapping("/types")
    @Operation(summary = "获取 Handler 类型列表", description = "返回所有已注册的 Handler 类型")
    public Result<List<String>> listHandlerTypes() {
        log.info("查询 Handler 类型列表");

        List<String> types = handlerRegistryService.getAllHandlerTypes();

        return Result.success(types);
    }

    /**
     * 获取 Handler 分类列表
     */
    @GetMapping("/categories")
    @Operation(summary = "获取 Handler 分类列表", description = "返回所有 Handler 的分类")
    public Result<List<String>> listCategories() {
        log.info("查询 Handler 分类列表");

        List<HandlerMetadata> metadataList = handlerRegistryService.getAllHandlerMetadata();

        List<String> categories = metadataList.stream()
                .map(HandlerMetadata::category)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        return Result.success(categories);
    }

    /**
     * 检查 Handler 是否存在
     */
    @GetMapping("/{handlerType}/exists")
    @Operation(summary = "检查 Handler 是否存在", description = "验证指定类型的 Handler 是否已注册")
    public Result<Boolean> checkHandlerExists(
        @Parameter(description = "Handler 类型") @PathVariable String handlerType
    ) {
        log.debug("检查 Handler 是否存在: handlerType={}", handlerType);

        boolean exists = handlerRegistryService.exists(handlerType);

        return Result.success(exists);
    }

    /**
     * 将 HandlerMetadata 转换为 VO
     */
    private HandlerMetadataVO convertToVO(HandlerMetadata metadata) {
        List<HandlerParamVO> paramVOs = new ArrayList<>();

        if (metadata.params() != null && metadata.params().length > 0) {
            paramVOs = Arrays.stream(metadata.params())
                    .map(this::convertParamToVO)
                    .collect(Collectors.toList());
        }

        return HandlerMetadataVO.builder()
                .handlerType(metadata.handlerType())
                .name(metadata.name())
                .description(metadata.description())
                .category(metadata.category())
                .enabled(true) // 所有注册的 Handler 默认启用
                .params(paramVOs)
                .build();
    }

    /**
     * 将 HandlerParam 转换为 VO
     */
    private HandlerParamVO convertParamToVO(HandlerParam param) {
        List<String> enumValues = null;
        if (param.enumValues() != null && param.enumValues().length > 0) {
            enumValues = Arrays.asList(param.enumValues());
        }

        return HandlerParamVO.builder()
                .name(param.name())
                .displayName(param.displayName())
                .type(param.type())
                .required(param.required())
                .defaultValue(param.defaultValue().isEmpty() ? null : param.defaultValue())
                .description(param.description())
                .enumValues(enumValues)
                .build();
    }
}
