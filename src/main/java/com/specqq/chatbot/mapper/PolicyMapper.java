package com.specqq.chatbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.specqq.chatbot.entity.RulePolicy;
import org.apache.ibatis.annotations.Mapper;

/**
 * RulePolicy Mapper接口
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Mapper
public interface PolicyMapper extends BaseMapper<RulePolicy> {
    // 继承 BaseMapper 的 CRUD 方法
}
