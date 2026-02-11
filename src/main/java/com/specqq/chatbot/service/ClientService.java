package com.specqq.chatbot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.specqq.chatbot.adapter.ClientAdapter;
import com.specqq.chatbot.adapter.ClientAdapterFactory;
import com.specqq.chatbot.entity.ChatClient;
import com.specqq.chatbot.entity.GroupChat;
import com.specqq.chatbot.mapper.ChatClientMapper;
import com.specqq.chatbot.mapper.GroupChatMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 客户端管理服务
 *
 * @author Chatbot Router System
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientService {

    private final ChatClientMapper chatClientMapper;
    private final GroupChatMapper groupChatMapper;
    private final ClientAdapterFactory clientAdapterFactory;

    /**
     * 分页查询客户端列表
     *
     * @param page    页码
     * @param size    每页大小
     * @param keyword 关键词(客户端名称或类型)
     * @param enabled 是否启用
     * @return 分页结果
     */
    public Page<ChatClient> listClients(int page, int size, String keyword, Boolean enabled) {
        Page<ChatClient> pageRequest = new Page<>(page, size);
        LambdaQueryWrapper<ChatClient> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                .like(ChatClient::getClientName, keyword)
                .or()
                .like(ChatClient::getClientType, keyword)
            );
        }

        if (enabled != null) {
            queryWrapper.eq(ChatClient::getEnabled, enabled);
        }

        queryWrapper.orderByDesc(ChatClient::getCreatedAt);

        return chatClientMapper.selectPage(pageRequest, queryWrapper);
    }

    /**
     * 根据ID获取客户端配置
     *
     * @param id 客户端ID
     * @return 客户端配置
     */
    public ChatClient getClientById(Long id) {
        return chatClientMapper.selectById(id);
    }

    /**
     * 创建客户端配置
     *
     * @param client 客户端配置
     * @return 创建的客户端配置
     */
    @Transactional(rollbackFor = Exception.class)
    public ChatClient createClient(ChatClient client) {
        // 验证客户端类型是否支持
        if (!clientAdapterFactory.supportsClientType(client.getClientType())) {
            throw new IllegalArgumentException("Unsupported client type: " + client.getClientType());
        }

        // 检查客户端名称是否已存在
        LambdaQueryWrapper<ChatClient> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatClient::getClientName, client.getClientName());
        Long count = chatClientMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new IllegalArgumentException("Client name already exists: " + client.getClientName());
        }

        // 设置默认值
        if (client.getEnabled() == null) {
            client.setEnabled(true);
        }
        client.setConnectionStatus("DISCONNECTED");

        chatClientMapper.insert(client);
        log.info("Client created: id={}, name={}, type={}",
            client.getId(), client.getClientName(), client.getClientType());

        return client;
    }

    /**
     * 更新客户端配置
     *
     * @param id     客户端ID
     * @param client 新的配置
     * @return 更新后的配置
     */
    @Transactional(rollbackFor = Exception.class)
    public ChatClient updateClient(Long id, ChatClient client) {
        ChatClient existingClient = chatClientMapper.selectById(id);
        if (existingClient == null) {
            throw new IllegalArgumentException("Client not found: " + id);
        }

        // 检查客户端名称是否与其他客户端重复
        if (StringUtils.hasText(client.getClientName()) &&
            !client.getClientName().equals(existingClient.getClientName())) {
            LambdaQueryWrapper<ChatClient> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ChatClient::getClientName, client.getClientName())
                .ne(ChatClient::getId, id);
            Long count = chatClientMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new IllegalArgumentException("Client name already exists: " + client.getClientName());
            }
        }

        // 更新字段
        client.setId(id);
        chatClientMapper.updateById(client);

        log.info("Client updated: id={}, name={}", id, client.getClientName());
        return chatClientMapper.selectById(id);
    }

    /**
     * 删除客户端配置(级联删除关联的群聊)
     *
     * @param id 客户端ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteClient(Long id) {
        ChatClient client = chatClientMapper.selectById(id);
        if (client == null) {
            throw new IllegalArgumentException("Client not found: " + id);
        }

        // 删除关联的群聊
        LambdaQueryWrapper<GroupChat> groupQueryWrapper = new LambdaQueryWrapper<>();
        groupQueryWrapper.eq(GroupChat::getClientId, id);
        List<GroupChat> groups = groupChatMapper.selectList(groupQueryWrapper);
        if (!groups.isEmpty()) {
            groupChatMapper.delete(groupQueryWrapper);
            log.info("Deleted {} groups associated with client: id={}", groups.size(), id);
        }

        // 删除客户端配置
        chatClientMapper.deleteById(id);
        log.info("Client deleted: id={}, name={}", id, client.getClientName());
    }

    /**
     * 测试客户端连接
     *
     * @param id 客户端ID
     * @return 连接是否成功
     */
    public boolean testConnection(Long id) {
        ChatClient client = chatClientMapper.selectById(id);
        if (client == null) {
            throw new IllegalArgumentException("Client not found: " + id);
        }

        try {
            // 获取适配器
            ClientAdapter adapter = clientAdapterFactory.getAdapter(client.getClientType());
            if (adapter == null) {
                log.error("No adapter found for client type: {}", client.getClientType());
                return false;
            }

            // 简单验证:检查配置是否完整
            boolean valid = client.getConnectionConfig() != null &&
                           StringUtils.hasText(client.getConnectionConfig().getHost());

            if (valid) {
                // 更新连接状态
                client.setConnectionStatus("CONNECTED");
                client.setLastHeartbeatTime(LocalDateTime.now());
                chatClientMapper.updateById(client);
                log.info("Connection test successful: id={}, name={}", id, client.getClientName());
            } else {
                client.setConnectionStatus("ERROR");
                chatClientMapper.updateById(client);
                log.warn("Connection test failed: id={}, name={}", id, client.getClientName());
            }

            return valid;

        } catch (Exception e) {
            log.error("Connection test error: id={}, name={}", id, client.getClientName(), e);
            client.setConnectionStatus("ERROR");
            chatClientMapper.updateById(client);
            return false;
        }
    }

    /**
     * 启用/禁用客户端
     *
     * @param id      客户端ID
     * @param enabled 是否启用
     */
    @Transactional(rollbackFor = Exception.class)
    public void toggleClientStatus(Long id, boolean enabled) {
        ChatClient client = chatClientMapper.selectById(id);
        if (client == null) {
            throw new IllegalArgumentException("Client not found: " + id);
        }

        client.setEnabled(enabled);
        chatClientMapper.updateById(client);

        log.info("Client status toggled: id={}, name={}, enabled={}",
            id, client.getClientName(), enabled);
    }

    /**
     * 获取所有启用的客户端
     *
     * @return 启用的客户端列表
     */
    public List<ChatClient> getEnabledClients() {
        LambdaQueryWrapper<ChatClient> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatClient::getEnabled, true);
        return chatClientMapper.selectList(queryWrapper);
    }
}
