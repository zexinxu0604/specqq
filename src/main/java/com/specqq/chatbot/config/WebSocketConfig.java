package com.specqq.chatbot.config;

import com.specqq.chatbot.websocket.NapCatWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

/**
 * WebSocket配置类
 *
 * @author Chatbot Router System
 */
@Slf4j
@Configuration
public class WebSocketConfig {

    @Lazy
    @Autowired
    private NapCatWebSocketHandler napCatWebSocketHandler;

    @Value("${napcat.websocket.url}")
    private String napCatWebSocketUrl;

    @Value("${napcat.websocket.access-token}")
    private String accessToken;

    @Bean
    public WebSocketClient webSocketClient() {
        return new StandardWebSocketClient();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void connectToNapCat() {
        // ApplicationContext 完全启动后再连接到 NapCat WebSocket
        new Thread(() -> {
            try {
                Thread.sleep(2000); // 给系统一点时间稳定
                napCatWebSocketHandler.connect();
            } catch (Exception e) {
                log.error("Failed to connect to NapCat on startup", e);
            }
        }).start();
    }
}
