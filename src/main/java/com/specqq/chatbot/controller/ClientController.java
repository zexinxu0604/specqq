package com.specqq.chatbot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.specqq.chatbot.common.Result;
import com.specqq.chatbot.entity.ChatClient;
import com.specqq.chatbot.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 客户端管理控制器
 *
 * @author Chatbot Router System
 */
@Slf4j
@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@Tag(name = "客户端管理", description = "多客户端配置管理API")
public class ClientController {

    private final ClientService clientService;

    /**
     * 分页查询客户端列表
     *
     * @param page    页码
     * @param size    每页大小
     * @param keyword 关键词(客户端名称或类型)
     * @param enabled 是否启用
     * @return 分页结果
     */
    @GetMapping
    @Operation(summary = "分页查询客户端列表", description = "支持关键词搜索和启用状态过滤")
    public Result<Page<ChatClient>> listClients(
        @Parameter(description = "页码", example = "1")
        @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "每页大小", example = "10")
        @RequestParam(defaultValue = "10") int size,
        @Parameter(description = "关键词(客户端名称或类型)")
        @RequestParam(required = false) String keyword,
        @Parameter(description = "是否启用")
        @RequestParam(required = false) Boolean enabled
    ) {
        try {
            Page<ChatClient> result = clientService.listClients(page, size, keyword, enabled);
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to list clients", e);
            return Result.error("查询客户端列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取客户端配置
     *
     * @param id 客户端ID
     * @return 客户端配置
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取客户端详情", description = "根据ID获取客户端配置")
    public Result<ChatClient> getClient(
        @Parameter(description = "客户端ID", required = true)
        @PathVariable Long id
    ) {
        try {
            ChatClient client = clientService.getClientById(id);
            if (client == null) {
                return Result.error("客户端不存在: " + id);
            }
            return Result.success(client);
        } catch (Exception e) {
            log.error("Failed to get client: id={}", id, e);
            return Result.error("获取客户端失败: " + e.getMessage());
        }
    }

    /**
     * 创建客户端配置
     *
     * @param client 客户端配置
     * @return 创建的客户端配置
     */
    @PostMapping
    @Operation(summary = "创建客户端", description = "创建新的客户端配置")
    public Result<ChatClient> createClient(
        @Parameter(description = "客户端配置", required = true)
        @Valid @RequestBody ChatClient client
    ) {
        try {
            ChatClient createdClient = clientService.createClient(client);
            return Result.success(createdClient);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to create client: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to create client", e);
            return Result.error("创建客户端失败: " + e.getMessage());
        }
    }

    /**
     * 更新客户端配置
     *
     * @param id     客户端ID
     * @param client 新的配置
     * @return 更新后的配置
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新客户端", description = "更新客户端配置")
    public Result<ChatClient> updateClient(
        @Parameter(description = "客户端ID", required = true)
        @PathVariable Long id,
        @Parameter(description = "客户端配置", required = true)
        @Valid @RequestBody ChatClient client
    ) {
        try {
            ChatClient updatedClient = clientService.updateClient(id, client);
            return Result.success(updatedClient);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to update client: id={}, error={}", id, e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to update client: id={}", id, e);
            return Result.error("更新客户端失败: " + e.getMessage());
        }
    }

    /**
     * 删除客户端配置
     *
     * @param id 客户端ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除客户端", description = "删除客户端配置(级联删除关联的群聊)")
    public Result<Void> deleteClient(
        @Parameter(description = "客户端ID", required = true)
        @PathVariable Long id
    ) {
        try {
            clientService.deleteClient(id);
            return Result.success("删除成功", null);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to delete client: id={}, error={}", id, e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to delete client: id={}", id, e);
            return Result.error("删除客户端失败: " + e.getMessage());
        }
    }

    /**
     * 测试客户端连接
     *
     * @param id 客户端ID
     * @return 连接测试结果
     */
    @PostMapping("/{id}/test")
    @Operation(summary = "测试客户端连接", description = "测试客户端配置是否可以正常连接")
    public Result<Boolean> testConnection(
        @Parameter(description = "客户端ID", required = true)
        @PathVariable Long id
    ) {
        try {
            boolean success = clientService.testConnection(id);
            if (success) {
                return Result.success("连接测试成功", true);
            } else {
                return Result.error("连接测试失败");
            }
        } catch (IllegalArgumentException e) {
            log.warn("Failed to test connection: id={}, error={}", id, e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to test connection: id={}", id, e);
            return Result.error("连接测试失败: " + e.getMessage());
        }
    }

    /**
     * 启用/禁用客户端
     *
     * @param id      客户端ID
     * @param enabled 是否启用
     * @return 操作结果
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "切换客户端状态", description = "启用或禁用客户端")
    public Result<Void> toggleStatus(
        @Parameter(description = "客户端ID", required = true)
        @PathVariable Long id,
        @Parameter(description = "是否启用", required = true)
        @RequestParam boolean enabled
    ) {
        try {
            clientService.toggleClientStatus(id, enabled);
            String message = enabled ? "客户端已启用" : "客户端已禁用";
            return Result.success(message, null);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to toggle client status: id={}, error={}", id, e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to toggle client status: id={}", id, e);
            return Result.error("切换客户端状态失败: " + e.getMessage());
        }
    }
}
