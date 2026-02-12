package com.specqq.chatbot.integration;

import com.specqq.chatbot.ChatbotApplication;
import com.specqq.chatbot.dto.MessageReceiveDTO;
import com.specqq.chatbot.engine.RuleEngine;
import com.specqq.chatbot.entity.GroupChat;
import com.specqq.chatbot.entity.GroupRuleConfig;
import com.specqq.chatbot.entity.MessageRule;
import com.specqq.chatbot.mapper.GroupChatMapper;
import com.specqq.chatbot.mapper.GroupRuleConfigMapper;
import com.specqq.chatbot.mapper.MessageRuleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for CQ Code rule matching
 *
 * <p>Tests end-to-end CQ code pattern matching with real database and rule engine.</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@SpringBootTest(classes = ChatbotApplication.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("CQ Code Integration Tests")
class CQCodeIntegrationTest {

    @Autowired
    private RuleEngine ruleEngine;

    @Autowired
    private MessageRuleMapper messageRuleMapper;

    @Autowired
    private GroupChatMapper groupChatMapper;

    @Autowired
    private GroupRuleConfigMapper groupRuleConfigMapper;

    private GroupChat testGroup;
    private MessageRule imageRule;

    @BeforeEach
    void setUp() {
        // Create test group
        testGroup = new GroupChat();
        testGroup.setGroupId("987654");
        testGroup.setGroupName("CQ Code Test Group");
        testGroup.setEnabled(true);
        testGroup.setCreatedAt(LocalDateTime.now());
        groupChatMapper.insert(testGroup);

        // Create image pattern rule
        imageRule = new MessageRule();
        imageRule.setName("Contains Image Rule");
        imageRule.setDescription("Matches messages containing image CQ codes");
        imageRule.setMatchType(MessageRule.MatchType.REGEX);
        imageRule.setPattern("\\[CQ:image");
        imageRule.setResponseTemplate("检测到图片消息");
        imageRule.setPriority(80);
        imageRule.setEnabled(true);
        imageRule.setCreatedBy(1L);
        imageRule.setCreateTime(LocalDateTime.now());
        messageRuleMapper.insert(imageRule);

        // Bind rule to group
        GroupRuleConfig config = new GroupRuleConfig();
        config.setGroupId(testGroup.getId());
        config.setRuleId(imageRule.getId());
        config.setCreatedAt(LocalDateTime.now());
        groupRuleConfigMapper.insert(config);
    }

    /**
     * T062: Test matching message with image CQ code
     */
    @Test
    @DisplayName("should_MatchImageMessage_When_ContainsImagePatternConfigured")
    void should_MatchImageMessage_When_ContainsImagePatternConfigured() {
        // Given: Message with image CQ code
        MessageReceiveDTO message = new MessageReceiveDTO();
        message.setGroupId("987654");
        message.setUserId("111111");
        message.setUserNickname("Test User");
        message.setMessageContent("看这张图片[CQ:image,file=abc.jpg,url=https://example.com/abc.jpg]");
        message.setTimestamp(LocalDateTime.now());

        // When: Matching rules
        Optional<MessageRule> matchedRule = ruleEngine.matchRules(message);

        // Then: Should match the image rule
        assertThat(matchedRule).isPresent();
        assertThat(matchedRule.get().getId()).isEqualTo(imageRule.getId());
        assertThat(matchedRule.get().getName()).isEqualTo("Contains Image Rule");
        assertThat(matchedRule.get().getResponseTemplate()).isEqualTo("检测到图片消息");
    }

    @Test
    @DisplayName("should_NotMatchImageRule_When_NoImageInMessage")
    void should_NotMatchImageRule_When_NoImageInMessage() {
        // Given: Message without image CQ code
        MessageReceiveDTO message = new MessageReceiveDTO();
        message.setGroupId("987654");
        message.setUserId("111111");
        message.setUserNickname("Test User");
        message.setMessageContent("纯文本消息，没有图片");
        
        message.setTimestamp(LocalDateTime.now());

        // When: Matching rules
        Optional<MessageRule> matchedRule = ruleEngine.matchRules(message);

        // Then: Should not match any rule
        assertThat(matchedRule).isEmpty();
    }

    @Test
    @DisplayName("should_MatchFaceRule_When_FacePatternConfigured")
    void should_MatchFaceRule_When_FacePatternConfigured() {
        // Given: Face pattern rule
        MessageRule faceRule = new MessageRule();
        faceRule.setName("Contains Face Rule");
        faceRule.setMatchType(MessageRule.MatchType.REGEX);
        faceRule.setPattern("\\[CQ:face");
        faceRule.setResponseTemplate("检测到表情");
        faceRule.setPriority(75);
        faceRule.setEnabled(true);
        faceRule.setCreatedBy(1L);
        faceRule.setCreateTime(LocalDateTime.now());
        messageRuleMapper.insert(faceRule);
        GroupRuleConfig faceConfig = new GroupRuleConfig();
        faceConfig.setGroupId(testGroup.getId());
        faceConfig.setRuleId(faceRule.getId());
        faceConfig.setCreatedAt(LocalDateTime.now());
        groupRuleConfigMapper.insert(faceConfig);

        // Given: Message with face CQ code
        MessageReceiveDTO message = new MessageReceiveDTO();
        message.setGroupId("987654");
        message.setUserId("111111");
        message.setMessageContent("Hello[CQ:face,id=123]");
        
        message.setTimestamp(LocalDateTime.now());

        // When: Matching rules
        Optional<MessageRule> matchedRule = ruleEngine.matchRules(message);

        // Then: Should match the face rule (higher priority)
        assertThat(matchedRule).isPresent();
        assertThat(matchedRule.get().getName()).isEqualTo("Contains Image Rule"); // Image rule has priority 80
    }

    @Test
    @DisplayName("should_MatchAtRule_When_AtPatternWithParamConfigured")
    void should_MatchAtRule_When_AtPatternWithParamConfigured() {
        // Given: At pattern rule with specific QQ number
        MessageRule atRule = new MessageRule();
        atRule.setName("At Admin Rule");
        atRule.setMatchType(MessageRule.MatchType.REGEX);
        atRule.setPattern("\\[CQ:at,qq=123456\\]");
        atRule.setResponseTemplate("管理员被@了");
        atRule.setPriority(90);
        atRule.setEnabled(true);
        atRule.setCreatedBy(1L);
        atRule.setCreateTime(LocalDateTime.now());
        messageRuleMapper.insert(atRule);
        GroupRuleConfig atConfig = new GroupRuleConfig();
        atConfig.setGroupId(testGroup.getId());
        atConfig.setRuleId(atRule.getId());
        atConfig.setCreatedAt(LocalDateTime.now());
        groupRuleConfigMapper.insert(atConfig);

        // Given: Message with at CQ code
        MessageReceiveDTO message = new MessageReceiveDTO();
        message.setGroupId("987654");
        message.setUserId("111111");
        message.setMessageContent("[CQ:at,qq=123456]请处理");
        
        message.setTimestamp(LocalDateTime.now());

        // When: Matching rules
        Optional<MessageRule> matchedRule = ruleEngine.matchRules(message);

        // Then: Should match the at rule
        assertThat(matchedRule).isPresent();
        assertThat(matchedRule.get().getName()).isEqualTo("At Admin Rule");
    }

    @Test
    @DisplayName("should_NotMatchAtRule_When_DifferentQQNumber")
    void should_NotMatchAtRule_When_DifferentQQNumber() {
        // Given: At pattern rule with specific QQ number
        MessageRule atRule = new MessageRule();
        atRule.setName("At Admin Rule");
        atRule.setMatchType(MessageRule.MatchType.REGEX);
        atRule.setPattern("\\[CQ:at,qq=123456\\]");
        atRule.setResponseTemplate("管理员被@了");
        atRule.setPriority(90);
        atRule.setEnabled(true);
        atRule.setCreatedBy(1L);
        atRule.setCreateTime(LocalDateTime.now());
        messageRuleMapper.insert(atRule);
        GroupRuleConfig atConfig2 = new GroupRuleConfig();
        atConfig2.setGroupId(testGroup.getId());
        atConfig2.setRuleId(atRule.getId());
        atConfig2.setCreatedAt(LocalDateTime.now());
        groupRuleConfigMapper.insert(atConfig2);

        // Given: Message with different QQ number
        MessageReceiveDTO message = new MessageReceiveDTO();
        message.setGroupId("987654");
        message.setUserId("111111");
        message.setMessageContent("[CQ:at,qq=999999]请处理");
        
        message.setTimestamp(LocalDateTime.now());

        // When: Matching rules
        Optional<MessageRule> matchedRule = ruleEngine.matchRules(message);

        // Then: Should match the image rule instead (lower priority)
        assertThat(matchedRule).isEmpty(); // No image in message, so no match
    }
}
