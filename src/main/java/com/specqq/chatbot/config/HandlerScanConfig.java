package com.specqq.chatbot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Handler 扫描配置
 *
 * <p>确保 Spring 自动扫描 handler 包下的所有 @HandlerMetadata 注解的类</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Slf4j
@Configuration
@ComponentScan(basePackages = {
        "com.specqq.chatbot.handler",
        "com.specqq.chatbot.interceptor"
})
public class HandlerScanConfig {

    public HandlerScanConfig() {
        log.info("Handler 扫描配置已加载: basePackages=[com.specqq.chatbot.handler, com.specqq.chatbot.interceptor]");
    }
}
