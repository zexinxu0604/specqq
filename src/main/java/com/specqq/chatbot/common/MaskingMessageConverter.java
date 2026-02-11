package com.specqq.chatbot.common;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日志敏感信息脱敏转换器
 *
 * 脱敏规则:
 * - 密码: password=xxx → password=***
 * - Token: token=xxx → token=***
 * - 手机号: 13812345678 → 138****5678
 * - 身份证: 110101199001011234 → 110101********1234
 *
 * @author Chatbot Router System
 */
public class MaskingMessageConverter extends MessageConverter {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(password|pwd|pass)\\s*[=:]\\s*[^\\s,;]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOKEN_PATTERN = Pattern.compile("(token|accessToken|access_token)\\s*[=:]\\s*[^\\s,;]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_PATTERN = Pattern.compile("(1[3-9]\\d)\\d{4}(\\d{4})");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(\\d{6})\\d{8}(\\d{4})");

    @Override
    public String convert(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        if (message == null) {
            return null;
        }

        // 脱敏密码
        Matcher passwordMatcher = PASSWORD_PATTERN.matcher(message);
        message = passwordMatcher.replaceAll("$1=***");

        // 脱敏Token
        Matcher tokenMatcher = TOKEN_PATTERN.matcher(message);
        message = tokenMatcher.replaceAll("$1=***");

        // 脱敏手机号
        Matcher phoneMatcher = PHONE_PATTERN.matcher(message);
        message = phoneMatcher.replaceAll("$1****$2");

        // 脱敏身份证
        Matcher idCardMatcher = ID_CARD_PATTERN.matcher(message);
        message = idCardMatcher.replaceAll("$1********$2");

        return message;
    }
}
