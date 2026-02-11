package com.specqq.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * NapCat消息DTO (OneBot 11协议)
 *
 * @author Chatbot Router System
 */
@Data
public class NapCatMessageDTO {

    /**
     * 消息类型 (message)
     */
    @JsonProperty("post_type")
    private String postType;

    /**
     * 消息类型 (group/private)
     */
    @JsonProperty("message_type")
    private String messageType;

    /**
     * 群号
     */
    @JsonProperty("group_id")
    private Long groupId;

    /**
     * 用户QQ号
     */
    @JsonProperty("user_id")
    private Long userId;

    /**
     * 消息ID
     */
    @JsonProperty("message_id")
    private Long messageId;

    /**
     * 消息内容(CQ码格式或纯文本)
     */
    @JsonProperty("raw_message")
    private String rawMessage;

    /**
     * 消息内容(数组格式)
     */
    @JsonProperty("message")
    private Object message;

    /**
     * 发送者信息
     */
    @JsonProperty("sender")
    private Sender sender;

    /**
     * 发送者信息
     */
    @Data
    public static class Sender {
        /**
         * 用户QQ号
         */
        @JsonProperty("user_id")
        private Long userId;

        /**
         * 昵称
         */
        @JsonProperty("nickname")
        private String nickname;

        /**
         * 群名片
         */
        @JsonProperty("card")
        private String card;

        /**
         * 角色 (owner/admin/member)
         */
        @JsonProperty("role")
        private String role;
    }

    /**
     * 获取显示昵称(优先群名片)
     */
    public String getDisplayName() {
        if (sender == null) {
            return "未知用户";
        }

        if (sender.getCard() != null && !sender.getCard().trim().isEmpty()) {
            return sender.getCard();
        }

        if (sender.getNickname() != null && !sender.getNickname().trim().isEmpty()) {
            return sender.getNickname();
        }

        return String.valueOf(sender.getUserId());
    }
}
