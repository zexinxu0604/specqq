package com.specqq.chatbot.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus配置类
 *
 * @author Chatbot Router System
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * MyBatis-Plus拦截器配置
     * - 分页插件: 支持MySQL分页查询
     * - 乐观锁插件: 支持@Version注解的乐观锁控制
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分页插件
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInterceptor.setMaxLimit(1000L); // 单页最大限制1000条
        paginationInterceptor.setOverflow(false); // 溢出总页数后是否进行处理(默认不处理)
        interceptor.addInnerInterceptor(paginationInterceptor);

        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        return interceptor;
    }

    /**
     * 自动填充处理器
     * - INSERT: 自动填充created_at, updated_at
     * - UPDATE: 自动填充updated_at
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now();

                // 自动填充created_at字段 (支持createdAt和createTime两种命名)
                this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);

                // 自动填充updated_at字段 (支持updatedAt和updateTime两种命名)
                this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);

                // 自动填充timestamp字段(MessageLog)
                this.strictInsertFill(metaObject, "timestamp", LocalDateTime.class, now);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now();

                // 自动填充updated_at字段 (支持updatedAt和updateTime两种命名)
                this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, now);
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, now);
            }
        };
    }
}
