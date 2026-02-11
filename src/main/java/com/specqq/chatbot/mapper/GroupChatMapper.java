package com.specqq.chatbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.specqq.chatbot.entity.GroupChat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * GroupChat Mapper接口
 *
 * @author Chatbot Router System
 */
@Mapper
public interface GroupChatMapper extends BaseMapper<GroupChat> {

    /**
     * 查询客户端下的启用群聊及其规则列表
     *
     * @param clientId 客户端ID
     * @param enabled  是否启用
     * @return 群聊列表(含enabledRules字段)
     */
    List<GroupChat> selectWithRules(@Param("clientId") Long clientId,
                                     @Param("enabled") Boolean enabled);
}
