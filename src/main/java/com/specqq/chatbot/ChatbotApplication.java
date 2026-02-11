package com.specqq.chatbot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 聊天机器人路由系统 - 主应用类
 *
 * @author Chatbot Router System
 * @version 1.0.0
 * @since 2026-02-09
 */
@SpringBootApplication
@MapperScan("com.specqq.chatbot.mapper")
@EnableAsync
@EnableScheduling
public class ChatbotApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatbotApplication.class, args);
    }
}
