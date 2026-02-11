package com.specqq.chatbot.handler;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Handler 元数据注解
 *
 * <p>用于标注消息处理器类，提供元数据信息供前端展示和配置</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface HandlerMetadata {

    /**
     * Handler 类型标识（唯一）
     * <p>用于规则配置时指定使用哪个 handler</p>
     */
    String handlerType();

    /**
     * Handler 显示名称
     * <p>前端展示用，支持中文</p>
     */
    String name();

    /**
     * Handler 描述
     * <p>说明此 handler 的功能和用途</p>
     */
    String description();

    /**
     * Handler 分类
     * <p>用于前端分组展示，如：工具、娱乐、管理</p>
     */
    String category() default "通用";

    /**
     * Handler 参数定义
     * <p>定义此 handler 需要的配置参数</p>
     */
    HandlerParam[] params() default {};

    /**
     * 是否启用
     * <p>禁用的 handler 不会在前端显示</p>
     */
    boolean enabled() default true;
}
