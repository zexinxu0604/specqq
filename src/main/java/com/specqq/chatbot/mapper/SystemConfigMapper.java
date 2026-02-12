package com.specqq.chatbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.specqq.chatbot.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 系统配置 Mapper
 * 提供系统配置的数据库访问操作
 *
 * @author Claude Code
 * @since 2026-02-12
 */
@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {

    /**
     * 根据配置键查询配置
     *
     * @param configKey 配置键
     * @return 配置实体，如果不存在则返回 null
     */
    SystemConfig selectByConfigKey(@Param("configKey") String configKey);

    /**
     * 更新配置值
     *
     * @param configKey   配置键
     * @param configValue 新配置值（JSON字符串）
     * @return 更新记录数
     */
    int updateConfigValue(@Param("configKey") String configKey,
                          @Param("configValue") String configValue);
}
