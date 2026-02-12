package com.specqq.chatbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.specqq.chatbot.entity.HandlerChain;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * HandlerChain Mapper接口
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Mapper
public interface HandlerChainMapper extends BaseMapper<HandlerChain> {

    /**
     * 查询规则的 handler 链，按顺序排序
     *
     * @param ruleId 规则ID
     * @return Handler链列表(按sequence_order ASC排序)
     */
    List<HandlerChain> selectByRuleIdOrdered(@Param("ruleId") Long ruleId);
}
