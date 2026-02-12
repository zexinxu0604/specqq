package com.specqq.chatbot.integration;

import com.specqq.chatbot.dto.BatchSyncResultDTO;
import com.specqq.chatbot.dto.GroupSyncResultDTO;
import com.specqq.chatbot.entity.ChatClient;
import com.specqq.chatbot.entity.GroupChat;
import com.specqq.chatbot.enums.SyncStatus;
import com.specqq.chatbot.mapper.ChatClientMapper;
import com.specqq.chatbot.mapper.GroupChatMapper;
import com.specqq.chatbot.service.GroupSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GroupSyncService 集成测试
 * 测试与数据库的实际交互
 *
 * @author Claude Code
 * @since 2026-02-12
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("群组同步服务集成测试")
class GroupSyncServiceIntegrationTest {

    @Autowired
    private GroupSyncService groupSyncService;

    @Autowired
    private GroupChatMapper groupChatMapper;

    @Autowired
    private ChatClientMapper chatClientMapper;

    private ChatClient testClient;
    private GroupChat testGroup;

    @BeforeEach
    void setUp() {
        // 创建测试客户端
        testClient = new ChatClient();
        testClient.setClientName("Test NapCat Client");
        testClient.setProtocolType("napcat");
        testClient.setHost("http://localhost:3000");
        testClient.setEnabled(true);
        chatClientMapper.insert(testClient);

        // 创建测试群组
        testGroup = new GroupChat();
        testGroup.setGroupId("123456789");
        testGroup.setGroupName("Test Group");
        testGroup.setClientId(testClient.getId());
        testGroup.setMemberCount(50);
        testGroup.setEnabled(true);
        testGroup.setActive(true);
        testGroup.setSyncStatus(SyncStatus.SUCCESS);
        groupChatMapper.insert(testGroup);
    }

    @Test
    @DisplayName("更新群组同步状态 - 数据库持久化")
    void testUpdateSyncStatus_DatabasePersistence() {
        // Given
        testGroup.markSyncFailure("Test failure reason");

        // When
        groupSyncService.updateSyncStatus(testGroup);

        // Then - 从数据库重新查询验证
        GroupChat updated = groupChatMapper.selectById(testGroup.getId());
        assertThat(updated).isNotNull();
        assertThat(updated.getSyncStatus()).isEqualTo(SyncStatus.FAILED);
        assertThat(updated.getFailureReason()).isEqualTo("Test failure reason");
        assertThat(updated.getConsecutiveFailureCount()).isEqualTo(1);
        assertThat(updated.getLastFailureTime()).isNotNull();
    }

    @Test
    @DisplayName("批量更新同步状态")
    void testBatchUpdateSyncStatus() {
        // Given - 创建多个群组
        GroupChat group2 = new GroupChat();
        group2.setGroupId("987654321");
        group2.setGroupName("Test Group 2");
        group2.setClientId(testClient.getId());
        group2.setEnabled(true);
        group2.setActive(true);
        groupChatMapper.insert(group2);

        // 标记失败
        testGroup.markSyncFailure("Failure 1");
        group2.markSyncFailure("Failure 2");

        // When
        Integer updated = groupSyncService.batchUpdateSyncStatus(List.of(testGroup, group2));

        // Then
        assertThat(updated).isEqualTo(2);

        // 验证数据库
        GroupChat dbGroup1 = groupChatMapper.selectById(testGroup.getId());
        GroupChat dbGroup2 = groupChatMapper.selectById(group2.getId());
        assertThat(dbGroup1.getSyncStatus()).isEqualTo(SyncStatus.FAILED);
        assertThat(dbGroup2.getSyncStatus()).isEqualTo(SyncStatus.FAILED);
    }

    @Test
    @DisplayName("获取告警群组 - 查询失败次数 >= 3 的群组")
    void testGetAlertGroups() {
        // Given - 创建失败次数不同的群组
        testGroup.setConsecutiveFailureCount(3);
        testGroup.setSyncStatus(SyncStatus.FAILED);
        groupChatMapper.updateById(testGroup);

        GroupChat group2 = new GroupChat();
        group2.setGroupId("111111");
        group2.setGroupName("Group 2");
        group2.setClientId(testClient.getId());
        group2.setEnabled(true);
        group2.setActive(true);
        group2.setConsecutiveFailureCount(5);
        group2.setSyncStatus(SyncStatus.FAILED);
        groupChatMapper.insert(group2);

        GroupChat group3 = new GroupChat();
        group3.setGroupId("222222");
        group3.setGroupName("Group 3");
        group3.setClientId(testClient.getId());
        group3.setEnabled(true);
        group3.setActive(true);
        group3.setConsecutiveFailureCount(1); // Below threshold
        group3.setSyncStatus(SyncStatus.FAILED);
        groupChatMapper.insert(group3);

        // When
        List<GroupChat> alertGroups = groupSyncService.getAlertGroups();

        // Then
        assertThat(alertGroups).hasSize(2);
        assertThat(alertGroups).allMatch(g -> g.getConsecutiveFailureCount() >= 3);
        assertThat(alertGroups).extracting(GroupChat::getGroupId)
                .containsExactlyInAnyOrder("123456789", "111111");
    }

    @Test
    @DisplayName("重置失败计数 - 数据库更新")
    void testResetFailureCount() {
        // Given
        testGroup.setConsecutiveFailureCount(5);
        testGroup.setFailureReason("Multiple failures");
        testGroup.setSyncStatus(SyncStatus.FAILED);
        groupChatMapper.updateById(testGroup);

        // When
        groupSyncService.resetFailureCount(testGroup.getId());

        // Then
        GroupChat updated = groupChatMapper.selectById(testGroup.getId());
        assertThat(updated.getConsecutiveFailureCount()).isEqualTo(0);
        assertThat(updated.getFailureReason()).isNull();
    }

    @Test
    @DisplayName("查询活跃群组")
    void testSelectActiveGroups() {
        // Given - 创建多个群组，部分不活跃
        GroupChat inactiveGroup = new GroupChat();
        inactiveGroup.setGroupId("inactive123");
        inactiveGroup.setGroupName("Inactive Group");
        inactiveGroup.setClientId(testClient.getId());
        inactiveGroup.setEnabled(true);
        inactiveGroup.setActive(false); // Not active
        groupChatMapper.insert(inactiveGroup);

        GroupChat disabledGroup = new GroupChat();
        disabledGroup.setGroupId("disabled456");
        disabledGroup.setGroupName("Disabled Group");
        disabledGroup.setClientId(testClient.getId());
        disabledGroup.setEnabled(false); // Disabled
        disabledGroup.setActive(true);
        groupChatMapper.insert(disabledGroup);

        // When
        List<GroupChat> activeGroups = groupChatMapper.selectActiveGroups();

        // Then - 只应该返回 testGroup (enabled=true, active=true)
        assertThat(activeGroups).isNotEmpty();
        assertThat(activeGroups).allMatch(g -> g.getEnabled() && g.getActive());
        assertThat(activeGroups).anyMatch(g -> g.getGroupId().equals("123456789"));
        assertThat(activeGroups).noneMatch(g -> g.getGroupId().equals("inactive123"));
        assertThat(activeGroups).noneMatch(g -> g.getGroupId().equals("disabled456"));
    }

    @Test
    @DisplayName("查询失败群组")
    void testSelectFailedGroups() {
        // Given - 创建不同失败次数的群组
        testGroup.setConsecutiveFailureCount(2);
        testGroup.setSyncStatus(SyncStatus.FAILED);
        groupChatMapper.updateById(testGroup);

        GroupChat failedGroup = new GroupChat();
        failedGroup.setGroupId("failed789");
        failedGroup.setGroupName("Failed Group");
        failedGroup.setClientId(testClient.getId());
        failedGroup.setEnabled(true);
        failedGroup.setActive(true);
        failedGroup.setConsecutiveFailureCount(4);
        failedGroup.setSyncStatus(SyncStatus.FAILED);
        groupChatMapper.insert(failedGroup);

        // When - 查询失败次数 >= 3 的群组
        List<GroupChat> failedGroups = groupChatMapper.selectFailedGroups(3);

        // Then
        assertThat(failedGroups).hasSize(1);
        assertThat(failedGroups.get(0).getGroupId()).isEqualTo("failed789");
        assertThat(failedGroups.get(0).getConsecutiveFailureCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("群组同步成功后状态更新")
    void testMarkSyncSuccess() {
        // Given - 群组之前有失败记录
        testGroup.setConsecutiveFailureCount(3);
        testGroup.setFailureReason("Previous failure");
        testGroup.setSyncStatus(SyncStatus.FAILED);
        groupChatMapper.updateById(testGroup);

        // When - 标记同步成功
        testGroup.markSyncSuccess();
        groupSyncService.updateSyncStatus(testGroup);

        // Then
        GroupChat updated = groupChatMapper.selectById(testGroup.getId());
        assertThat(updated.getSyncStatus()).isEqualTo(SyncStatus.SUCCESS);
        assertThat(updated.getConsecutiveFailureCount()).isEqualTo(0);
        assertThat(updated.getFailureReason()).isNull();
        assertThat(updated.getLastSyncTime()).isNotNull();
    }

    @Test
    @DisplayName("群组同步失败后失败计数递增")
    void testMarkSyncFailure_IncrementCounter() {
        // Given - 群组已有2次失败
        testGroup.setConsecutiveFailureCount(2);
        testGroup.setSyncStatus(SyncStatus.FAILED);
        groupChatMapper.updateById(testGroup);

        // When - 再次失败
        testGroup.markSyncFailure("Third failure");
        groupSyncService.updateSyncStatus(testGroup);

        // Then
        GroupChat updated = groupChatMapper.selectById(testGroup.getId());
        assertThat(updated.getConsecutiveFailureCount()).isEqualTo(3);
        assertThat(updated.getSyncStatus()).isEqualTo(SyncStatus.FAILED);
        assertThat(updated.getFailureReason()).isEqualTo("Third failure");
        assertThat(updated.needsAlert()).isTrue();
    }
}
