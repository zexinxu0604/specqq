package com.specqq.chatbot.dto;

import com.specqq.chatbot.entity.MessageRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 规则匹配上下文
 *
 * @author Chatbot Router System
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleMatchContext {

    /**
     * 消息内容
     */
    private String message;

    /**
     * 群聊ID
     */
    private Long groupId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 匹配的规则
     */
    private MessageRule matchedRule;

    /**
     * 处理开始时间
     */
    private LocalDateTime processingStartTime;

    /**
     * 计算处理耗时(毫秒)
     */
    public Integer getProcessingTimeMs() {
        if (processingStartTime == null) {
            return null;
        }
        return (int) java.time.Duration.between(processingStartTime, LocalDateTime.now()).toMillis();
    }
}
