package com.specqq.chatbot.config;

import com.specqq.chatbot.common.constants.RuleConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 *
 * <p>配置消息路由线程池，用于异步执行 handler</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 消息路由线程池
     *
     * <p>核心线程数: 10</p>
     * <p>最大线程数: 50</p>
     * <p>队列容量: 100</p>
     * <p>超时时间: 30 秒</p>
     */
    @Bean(name = "messageRouterExecutor")
    public Executor messageRouterExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数
        executor.setCorePoolSize(RuleConstants.MESSAGE_ROUTER_CORE_POOL_SIZE);

        // 最大线程数
        executor.setMaxPoolSize(RuleConstants.MESSAGE_ROUTER_MAX_POOL_SIZE);

        // 队列容量
        executor.setQueueCapacity(RuleConstants.MESSAGE_ROUTER_QUEUE_CAPACITY);

        // 线程名称前缀
        executor.setThreadNamePrefix(RuleConstants.MESSAGE_ROUTER_THREAD_NAME_PREFIX);

        // 拒绝策略：由调用线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 线程池关闭时等待任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("消息路由线程池初始化完成: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                RuleConstants.MESSAGE_ROUTER_CORE_POOL_SIZE,
                RuleConstants.MESSAGE_ROUTER_MAX_POOL_SIZE,
                RuleConstants.MESSAGE_ROUTER_QUEUE_CAPACITY);

        return executor;
    }
}
