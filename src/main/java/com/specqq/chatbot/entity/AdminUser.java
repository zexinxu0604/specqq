package com.specqq.chatbot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员用户实体类
 *
 * @author Chatbot Router System
 * @tableName admin_user
 */
@Data
@TableName(value = "admin_user")
public class AdminUser {

    /**
     * 用户唯一标识
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名(唯一)
     */
    @TableField("username")
    private String username;

    /**
     * 密码(BCrypt加密, 12轮salt)
     */
    @TableField("password")
    private String password;

    /**
     * 邮箱
     */
    @TableField("email")
    private String email;

    /**
     * 用户角色
     */
    @TableField("role")
    private UserRole role;

    /**
     * 是否启用
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 最后登录时间
     */
    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 用户角色枚举
     */
    public enum UserRole {
        /**
         * 系统管理员(所有权限)
         */
        ADMIN,

        /**
         * 运营人员(规则配置权限)
         */
        OPERATOR,

        /**
         * 查看者(只读权限)
         */
        VIEWER
    }
}
