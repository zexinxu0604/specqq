package com.specqq.chatbot.service;

import com.specqq.chatbot.handler.HandlerMetadata;
import com.specqq.chatbot.handler.MessageHandler;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handler 注册服务
 *
 * <p>自动扫描和注册所有带 @HandlerMetadata 注解的 Handler</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Slf4j
@Service
public class HandlerRegistryService implements ApplicationContextAware {

    private ApplicationContext applicationContext;
    private final Map<String, MessageHandler> handlerRegistry = new HashMap<>();
    private final Map<String, HandlerMetadata> metadataRegistry = new HashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 初始化时自动注册所有 Handler
     */
    @PostConstruct
    public void registerHandlers() {
        log.info("开始注册 Handler...");

        // 获取所有 MessageHandler 的 Bean
        Map<String, MessageHandler> handlers = applicationContext.getBeansOfType(MessageHandler.class);

        for (Map.Entry<String, MessageHandler> entry : handlers.entrySet()) {
            MessageHandler handler = entry.getValue();
            Class<?> handlerClass = handler.getClass();

            // 检查是否有 @HandlerMetadata 注解
            HandlerMetadata metadata = handlerClass.getAnnotation(HandlerMetadata.class);
            if (metadata != null) {
                String handlerType = metadata.handlerType();

                // 注册 Handler 和元数据
                handlerRegistry.put(handlerType, handler);
                metadataRegistry.put(handlerType, metadata);

                log.info("注册 Handler: type={}, name={}, category={}",
                        handlerType, metadata.name(), metadata.category());
            }
        }

        log.info("Handler 注册完成，共注册 {} 个 Handler", handlerRegistry.size());
    }

    /**
     * 获取所有 Handler 元数据
     *
     * @return Handler 元数据列表
     */
    public List<HandlerMetadata> getAllHandlerMetadata() {
        return metadataRegistry.values().stream()
                .collect(Collectors.toList());
    }

    /**
     * 根据类型获取 Handler
     *
     * @param handlerType Handler 类型
     * @return Handler 实例，不存在返回 null
     */
    public MessageHandler getHandler(String handlerType) {
        return handlerRegistry.get(handlerType);
    }

    /**
     * 根据类型获取 Handler 元数据
     *
     * @param handlerType Handler 类型
     * @return Handler 元数据，不存在返回 null
     */
    public HandlerMetadata getHandlerMetadata(String handlerType) {
        return metadataRegistry.get(handlerType);
    }

    /**
     * 检查 Handler 是否存在
     *
     * @param handlerType Handler 类型
     * @return true 表示存在
     */
    public boolean exists(String handlerType) {
        return handlerRegistry.containsKey(handlerType);
    }

    /**
     * 获取已注册的 Handler 数量
     *
     * @return Handler 数量
     */
    public int getHandlerCount() {
        return handlerRegistry.size();
    }

    /**
     * 获取所有已注册的 Handler 类型
     *
     * @return Handler 类型列表
     */
    public List<String> getAllHandlerTypes() {
        return handlerRegistry.keySet().stream()
                .sorted()
                .collect(Collectors.toList());
    }
}
