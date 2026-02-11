package com.specqq.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 接收消息DTO
 *
 * @author Chatbot Router System
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageReceiveDTO {

    /**
     * 消息ID(平台唯一标识)
     */
    private String messageId;

    /**
     * 群聊ID(平台ID,如QQ群号)
     */
    private String groupId;

    /**
     * 用户ID(平台ID)
     */
    private String userId;

    /**
     * 用户昵称
     */
    private String userNickname;

    /**
     * 消息内容
     */
    private String messageContent;

    /**
     * 消息时间戳
     */
    private LocalDateTime timestamp;
}
