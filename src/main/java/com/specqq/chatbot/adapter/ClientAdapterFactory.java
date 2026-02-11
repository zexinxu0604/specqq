package com.specqq.chatbot.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户端适配器工厂
 * 使用策略模式管理多个客户端适配器
 *
 * @author Chatbot Router System
 */
@Slf4j
@Component
public class ClientAdapterFactory {

    /**
     * 适配器注册表: clientType -> ClientAdapter
     */
    private final Map<String, ClientAdapter> adapterRegistry = new HashMap<>();

    /**
     * Spring自动注入所有ClientAdapter实现
     */
    private final List<ClientAdapter> clientAdapters;

    @Autowired
    public ClientAdapterFactory(List<ClientAdapter> clientAdapters) {
        this.clientAdapters = clientAdapters;
    }

    /**
     * 初始化: 注册所有适配器
     */
    @PostConstruct
    public void init() {
        for (ClientAdapter adapter : clientAdapters) {
            String clientType = adapter.getClientType();
            adapterRegistry.put(clientType, adapter);
            log.info("Registered client adapter: type={}, class={}, protocols={}",
                clientType,
                adapter.getClass().getSimpleName(),
                adapter.getSupportedProtocols());
        }
        log.info("ClientAdapterFactory initialized with {} adapters", adapterRegistry.size());
    }

    /**
     * 根据客户端类型获取适配器
     *
     * @param clientType 客户端类型(如: qq, wechat, dingtalk)
     * @return 对应的适配器,如果不存在则返回null
     */
    public ClientAdapter getAdapter(String clientType) {
        ClientAdapter adapter = adapterRegistry.get(clientType);
        if (adapter == null) {
            log.warn("No adapter found for client type: {}", clientType);
        }
        return adapter;
    }

    /**
     * 检查是否支持指定的客户端类型
     *
     * @param clientType 客户端类型
     * @return 是否支持
     */
    public boolean supportsClientType(String clientType) {
        return adapterRegistry.containsKey(clientType);
    }

    /**
     * 获取所有已注册的客户端类型
     *
     * @return 客户端类型列表
     */
    public List<String> getSupportedClientTypes() {
        return List.copyOf(adapterRegistry.keySet());
    }

    /**
     * 获取所有已注册的适配器
     *
     * @return 适配器列表
     */
    public List<ClientAdapter> getAllAdapters() {
        return List.copyOf(adapterRegistry.values());
    }
}
