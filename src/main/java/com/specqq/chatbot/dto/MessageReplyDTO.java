package com.specqq.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 回复消息DTO
 *
 * @author Chatbot Router System
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageReplyDTO {

    /**
     * 群聊ID(平台ID)
     */
    private String groupId;

    /**
     * 回复内容
     */
    private String replyContent;

    /**
     * 引用的消息ID(可选)
     */
    private String messageId;
}
