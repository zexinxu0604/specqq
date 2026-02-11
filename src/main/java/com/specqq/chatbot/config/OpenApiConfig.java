package com.specqq.chatbot.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 配置 (Swagger UI)
 *
 * @author Chatbot Router System
 */
@Configuration
public class OpenApiConfig {

    /**
     * 配置OpenAPI文档
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            // API信息
            .info(new Info()
                .title("Chatbot Router System API")
                .description("聊天机器人路由系统 - QQ群消息自动回复\n\n" +
                    "## 功能特性\n" +
                    "- 🤖 规则引擎：支持关键词、正则、前缀、后缀、全匹配\n" +
                    "- 🎯 智能路由：优先级排序、频率限制、群聊配置\n" +
                    "- 📊 日志统计：消息日志、规则统计、用户统计\n" +
                    "- 🔐 JWT认证：Bearer Token、令牌刷新、黑名单\n" +
                    "- 🚀 高性能：Redis缓存、批量插入、异步处理\n\n" +
                    "## 认证说明\n" +
                    "大部分API需要JWT认证，请先调用 `/api/auth/login` 获取令牌，" +
                    "然后在请求头中添加: `Authorization: Bearer {token}`")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Chatbot Router Team")
                    .email("support@chatbot-router.local")
                    .url("https://github.com/chatbot-router"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))

            // 服务器配置
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("本地开发环境"),
                new Server()
                    .url("http://localhost:8080")
                    .description("生产环境")
            ))

            // 安全配置
            .components(new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT认证令牌\n\n" +
                        "使用步骤：\n" +
                        "1. 调用 POST /api/auth/login 获取令牌\n" +
                        "2. 在请求头添加: Authorization: Bearer {token}\n" +
                        "3. 令牌有效期24小时，可通过 POST /api/auth/refresh 刷新")))

            // 全局安全要求
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
