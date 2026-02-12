package com.specqq.chatbot.adapter;

import com.specqq.chatbot.dto.ApiCallResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * NapCat API Client
 *
 * <p>OneBot 11 protocol API client for querying group member information</p>
 * <p>Delegates to NapCatAdapter for actual HTTP/WebSocket communication</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NapCatApiClient {

    private final NapCatAdapter napCatAdapter;

    /**
     * Get group member information
     *
     * <p>T068: Query OneBot 11 API for group member details including role</p>
     *
     * <p>Response data structure (OneBot 11 protocol):</p>
     * <pre>
     * {
     *   "group_id": 123456789,
     *   "user_id": 987654321,
     *   "nickname": "用户昵称",
     *   "card": "群名片",
     *   "role": "owner" | "admin" | "member",
     *   "join_time": 1609459200,
     *   "last_sent_time": 1609545600
     * }
     * </pre>
     *
     * @param groupId QQ group ID
     * @param userId  QQ user ID
     * @return API response containing member info with role field
     */
    public CompletableFuture<GroupMemberInfo> getGroupMemberInfo(String groupId, String userId) {
        try {
            Long groupIdLong = Long.parseLong(groupId);
            Long userIdLong = Long.parseLong(userId);

            return napCatAdapter.getGroupMemberInfo(groupIdLong, userIdLong)
                    .thenApply(response -> {
                        if (response == null || response.getRetcode() != 0) {
                            log.warn("Failed to get group member info: groupId={}, userId={}, retcode={}",
                                    groupId, userId, response != null ? response.getRetcode() : "null");
                            return null;
                        }

                        Map<String, Object> data = response.getData();
                        if (data == null) {
                            log.warn("Empty data in group member info response: groupId={}, userId={}",
                                    groupId, userId);
                            return null;
                        }

                        // Parse OneBot 11 response
                        GroupMemberInfo memberInfo = new GroupMemberInfo();
                        memberInfo.setGroupId(String.valueOf(data.get("group_id")));
                        memberInfo.setUserId(String.valueOf(data.get("user_id")));
                        memberInfo.setNickname((String) data.get("nickname"));
                        memberInfo.setCard((String) data.get("card"));
                        memberInfo.setRole((String) data.get("role")); // "owner", "admin", "member"
                        memberInfo.setJoinTime(getLongValue(data, "join_time"));
                        memberInfo.setLastSentTime(getLongValue(data, "last_sent_time"));

                        log.debug("Retrieved group member info: groupId={}, userId={}, role={}",
                                groupId, userId, memberInfo.getRole());
                        return memberInfo;
                    })
                    .exceptionally(ex -> {
                        log.error("Exception while getting group member info: groupId={}, userId={}",
                                groupId, userId, ex);
                        return null;
                    });

        } catch (NumberFormatException e) {
            log.error("Invalid groupId or userId format: groupId={}, userId={}", groupId, userId, e);
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Helper method to safely extract Long values from response data
     */
    private Long getLongValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse {} as Long: {}", key, value);
            return null;
        }
    }

    /**
     * Group member information DTO
     *
     * <p>Represents OneBot 11 group member data structure</p>
     */
    public static class GroupMemberInfo {
        private String groupId;
        private String userId;
        private String nickname;
        private String card; // Group card (custom name in group)
        private String role; // "owner", "admin", "member"
        private Long joinTime;
        private Long lastSentTime;

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getCard() {
            return card;
        }

        public void setCard(String card) {
            this.card = card;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public Long getJoinTime() {
            return joinTime;
        }

        public void setJoinTime(Long joinTime) {
            this.joinTime = joinTime;
        }

        public Long getLastSentTime() {
            return lastSentTime;
        }

        public void setLastSentTime(Long lastSentTime) {
            this.lastSentTime = lastSentTime;
        }
    }
}
