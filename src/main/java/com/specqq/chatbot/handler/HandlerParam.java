package com.specqq.chatbot.handler;

import java.lang.annotation.*;

/**
 * Handler 参数定义注解
 *
 * <p>用于定义 handler 的配置参数，供前端动态生成配置表单</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HandlerParam {

    /**
     * 参数名称（字段名）
     * <p>用于 JSON 配置中的 key</p>
     */
    String name();

    /**
     * 参数显示名称
     * <p>前端表单标签，支持中文</p>
     */
    String displayName();

    /**
     * 参数类型
     * <p>支持: string, number, boolean, select, textarea</p>
     */
    String type() default "string";

    /**
     * 是否必填
     */
    boolean required() default false;

    /**
     * 默认值
     */
    String defaultValue() default "";

    /**
     * 参数描述
     * <p>显示在表单下方的提示文本</p>
     */
    String description() default "";

    /**
     * 枚举值列表（当 type=select 时使用）
     * <p>格式: ["选项1", "选项2", "选项3"]</p>
     */
    String[] enumValues() default {};
}
