package com.specqq.chatbot.event;

import com.specqq.chatbot.entity.GroupChat;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * 群组发现事件
 * 当系统发现新群组时触发此事件
 *
 * @author Claude Code
 * @since 2026-02-12
 */
@Getter
public class GroupDiscoveryEvent extends ApplicationEvent {

    /**
     * 新发现的群组列表
     */
    private final List<GroupChat> newGroups;

    /**
     * 客户端ID
     */
    private final Long clientId;

    public GroupDiscoveryEvent(Object source, List<GroupChat> newGroups, Long clientId) {
        super(source);
        this.newGroups = newGroups;
        this.clientId = clientId;
    }

    /**
     * 获取新群组数量
     *
     * @return 新群组数量
     */
    public int getNewGroupCount() {
        return newGroups != null ? newGroups.size() : 0;
    }
}
