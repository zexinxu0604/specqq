package com.specqq.chatbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.specqq.chatbot.entity.MessageRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MessageRule Mapper接口
 *
 * @author Chatbot Router System
 */
@Mapper
public interface MessageRuleMapper extends BaseMapper<MessageRule> {

    /**
     * 查询群聊启用的规则列表(按优先级降序)
     *
     * @param groupId 群聊ID
     * @return 规则列表(按priority DESC, created_at ASC排序)
     */
    List<MessageRule> selectEnabledRulesByGroupId(@Param("groupId") Long groupId);
}
