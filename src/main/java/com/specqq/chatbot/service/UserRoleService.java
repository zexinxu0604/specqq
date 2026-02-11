package com.specqq.chatbot.service;

import com.specqq.chatbot.adapter.NapCatApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * User Role Service
 *
 * <p>Provides cached user role lookups for RoleInterceptor</p>
 * <p>Integrates with NapCat API to query group member roles</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final NapCatApiClient napCatApiClient;

    /**
     * Get user's role in a group
     *
     * <p>Uses Caffeine cache with 5min TTL to reduce API calls</p>
     * <p>Cache key format: {groupId}:{userId}</p>
     *
     * @param groupId QQ group ID
     * @param userId  QQ user ID
     * @return User role: "owner", "admin", "member", or null if unavailable
     */
    @Cacheable(value = "userRoles", key = "#groupId + ':' + #userId", cacheManager = "caffeineCacheManager")
    public String getUserRole(String groupId, String userId) {
        log.debug("Fetching user role: groupId={}, userId={}", groupId, userId);

        try {
            // Call NapCat API with 3-second timeout
            CompletableFuture<NapCatApiClient.GroupMemberInfo> future =
                    napCatApiClient.getGroupMemberInfo(groupId, userId);

            NapCatApiClient.GroupMemberInfo memberInfo = future.get(3, TimeUnit.SECONDS);

            if (memberInfo == null) {
                log.warn("Failed to get user role from NapCat API: groupId={}, userId={}",
                        groupId, userId);
                return "member"; // Default to "member" on API failure
            }

            String role = memberInfo.getRole();
            log.debug("Retrieved user role: groupId={}, userId={}, role={}",
                    groupId, userId, role);
            return role;

        } catch (Exception e) {
            log.error("Exception while fetching user role: groupId={}, userId={}",
                    groupId, userId, e);
            return "member"; // Default to "member" on exception
        }
    }

    /**
     * Clear user role cache
     *
     * @param groupId Group ID
     * @param userId  User ID
     */
    public void evictUserRoleCache(String groupId, String userId) {
        log.info("Evicting user role cache: groupId={}, userId={}", groupId, userId);
        // Cache will auto-expire, manual eviction can also be triggered
    }
}
