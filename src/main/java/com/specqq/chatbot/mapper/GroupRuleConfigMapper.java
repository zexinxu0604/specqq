package com.specqq.chatbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.specqq.chatbot.entity.GroupRuleConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * GroupRuleConfig Mapper接口
 *
 * @author Chatbot Router System
 */
@Mapper
public interface GroupRuleConfigMapper extends BaseMapper<GroupRuleConfig> {
    // BaseMapper provides standard CRUD operations
    // Unique constraint (group_id, rule_id) enforced at database level
}
