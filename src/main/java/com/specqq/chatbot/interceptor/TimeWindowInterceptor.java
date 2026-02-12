package com.specqq.chatbot.interceptor;

import com.specqq.chatbot.dto.MessageReceiveDTO;
import com.specqq.chatbot.entity.RulePolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Time Window 拦截器
 *
 * <p>检查消息是否在规则的时间窗口内（工作日、活跃时间段）</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Slf4j
@Component
public class TimeWindowInterceptor implements PolicyInterceptor {

    private String interceptReason;

    @Override
    public boolean intercept(MessageReceiveDTO message, RulePolicy policy) {
        // 如果策略为空或未启用时间窗口，默认通过
        if (policy == null || !Boolean.TRUE.equals(policy.getTimeWindowEnabled())) {
            return true;
        }

        LocalTime now = LocalTime.now();
        DayOfWeek today = java.time.LocalDate.now().getDayOfWeek();

        // 检查工作日限制
        if (policy.getTimeWindowWeekdays() != null && !policy.getTimeWindowWeekdays().isEmpty()) {
            Set<Integer> allowedWeekdays = parseWeekdays(policy.getTimeWindowWeekdays());
            int todayValue = today.getValue(); // 1=Monday, 7=Sunday

            if (!allowedWeekdays.contains(todayValue)) {
                interceptReason = String.format("当前不在允许的工作日内: 今天=%s, 允许=%s",
                        today, policy.getTimeWindowWeekdays());
                log.debug("Time Window 拦截: {}", interceptReason);
                return false;
            }
        }

        // 检查时间段限制
        if (policy.getTimeWindowStart() != null && policy.getTimeWindowEnd() != null) {
            LocalTime start = policy.getTimeWindowStart();
            LocalTime end = policy.getTimeWindowEnd();

            // 处理跨午夜的情况
            boolean inWindow;
            if (start.isBefore(end)) {
                // 正常情况: 09:00-18:00
                inWindow = !now.isBefore(start) && !now.isAfter(end);
            } else {
                // 跨午夜情况: 22:00-06:00
                inWindow = !now.isBefore(start) || !now.isAfter(end);
            }

            if (!inWindow) {
                interceptReason = String.format("当前不在允许的时间段内: 当前=%s, 允许=%s-%s",
                        now, start, end);
                log.debug("Time Window 拦截: {}", interceptReason);
                return false;
            }
        }

        return true;
    }

    @Override
    public String getInterceptReason() {
        return interceptReason;
    }

    @Override
    public String getName() {
        return "TimeWindow";
    }

    /**
     * 解析工作日字符串
     *
     * 格式: "1,2,3,4,5" (1=Monday, 7=Sunday)
     */
    private Set<Integer> parseWeekdays(String weekdays) {
        return Arrays.stream(weekdays.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
    }
}
