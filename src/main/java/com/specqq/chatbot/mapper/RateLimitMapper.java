package com.specqq.chatbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.specqq.chatbot.entity.RateLimitWindow;
import org.apache.ibatis.annotations.Mapper;

/**
 * RateLimitWindow Mapper接口
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Mapper
public interface RateLimitMapper extends BaseMapper<RateLimitWindow> {

    /**
     * 清理过期记录（降级方案）
     *
     * @return 删除的记录数
     */
    int deleteExpiredRecords();
}
