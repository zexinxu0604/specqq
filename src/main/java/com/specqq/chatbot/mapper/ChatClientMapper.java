package com.specqq.chatbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.specqq.chatbot.entity.ChatClient;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天客户端Mapper
 *
 * @author Chatbot Router System
 */
@Mapper
public interface ChatClientMapper extends BaseMapper<ChatClient> {
}
