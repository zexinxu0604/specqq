package com.specqq.chatbot.handler;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.specqq.chatbot.dto.MessageReceiveDTO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Message Statistics Handler
 *
 * <p>Analyzes message content and counts:</p>
 * <ul>
 *   <li>Text characters (excluding CQ codes)</li>
 *   <li>Different types of CQ codes (emoji, image, at, reply, record, video, etc.)</li>
 * </ul>
 *
 * <p>Implements FR-003, FR-004 from spec 002-napcat-cqcode-parser</p>
 *
 * @author specqq
 * @since 2026-02-11
 */
@Slf4j
@Component
@HandlerMetadata(
    handlerType = "MESSAGE_STATISTICS",
    name = "消息统计",
    description = "统计消息中的文字数量和各类CQ码数量（表情、图片、@、回复、语音、视频等）",
    category = "信息查询",
    params = {
        @HandlerParam(
            name = "showZeroCounts",
            displayName = "显示零计数",
            type = "boolean",
            required = false,
            defaultValue = "false",
            description = "是否显示计数为0的项目（默认只显示非零项）"
        ),
        @HandlerParam(
            name = "format",
            displayName = "输出格式",
            type = "enum",
            required = false,
            defaultValue = "simple",
            enumValues = {"simple", "detailed", "json"},
            description = "统计结果的输出格式：simple=简洁格式, detailed=详细格式, json=JSON格式"
        )
    }
)
public class MessageStatisticsHandler extends BaseHandler {

    /**
     * CQ Code pattern: [CQ:type,param1=value1,param2=value2,...]
     * Supports both with and without parameters
     */
    private static final Pattern CQ_CODE_PATTERN = Pattern.compile("\\[CQ:([^,\\]]+)(?:,([^\\]]+))?\\]");

    /**
     * CQ Code type labels in Chinese
     */
    private static final Map<String, String> CQ_TYPE_LABELS = new HashMap<>();

    static {
        CQ_TYPE_LABELS.put("face", "表情");
        CQ_TYPE_LABELS.put("image", "图片");
        CQ_TYPE_LABELS.put("at", "@");
        CQ_TYPE_LABELS.put("reply", "回复");
        CQ_TYPE_LABELS.put("record", "语音");
        CQ_TYPE_LABELS.put("video", "视频");
        CQ_TYPE_LABELS.put("share", "分享");
        CQ_TYPE_LABELS.put("music", "音乐");
        CQ_TYPE_LABELS.put("location", "位置");
        CQ_TYPE_LABELS.put("shake", "戳一戳");
        CQ_TYPE_LABELS.put("poke", "戳一戳");
        CQ_TYPE_LABELS.put("gift", "礼物");
        CQ_TYPE_LABELS.put("forward", "转发");
        CQ_TYPE_LABELS.put("json", "JSON卡片");
        CQ_TYPE_LABELS.put("xml", "XML卡片");
    }

    /**
     * CQ Code type units in Chinese
     */
    private static final Map<String, String> CQ_TYPE_UNITS = new HashMap<>();

    static {
        CQ_TYPE_UNITS.put("face", "个");
        CQ_TYPE_UNITS.put("image", "张");
        CQ_TYPE_UNITS.put("at", "次");
        CQ_TYPE_UNITS.put("reply", "条");
        CQ_TYPE_UNITS.put("record", "段");
        CQ_TYPE_UNITS.put("video", "个");
        CQ_TYPE_UNITS.put("share", "条");
        CQ_TYPE_UNITS.put("music", "首");
        CQ_TYPE_UNITS.put("location", "个");
        CQ_TYPE_UNITS.put("shake", "次");
        CQ_TYPE_UNITS.put("poke", "次");
        CQ_TYPE_UNITS.put("gift", "个");
        CQ_TYPE_UNITS.put("forward", "条");
        CQ_TYPE_UNITS.put("json", "个");
        CQ_TYPE_UNITS.put("xml", "个");
    }

    @Override
    protected String process(MessageReceiveDTO message, Object params) {
        String messageContent = message.getMessageContent();

        if (messageContent == null || messageContent.isEmpty()) {
            return "消息为空";
        }

        // Parse parameters (BaseHandler already extracted the "params" field)
        StatisticsParams statsParams = null;
        if (params instanceof StatisticsParams) {
            statsParams = (StatisticsParams) params;
        } else if (params instanceof java.util.Map) {
            // If params is a Map, convert it to StatisticsParams
            try {
                statsParams = objectMapper.convertValue(params, StatisticsParams.class);
            } catch (Exception e) {
                log.warn("无法将 Map 转换为 StatisticsParams，使用默认值: {}", e.getMessage());
            }
        }

        boolean showZeroCounts = statsParams != null && statsParams.getShowZeroCounts() != null
                ? statsParams.getShowZeroCounts()
                : false;
        String format = statsParams != null && statsParams.getFormat() != null
                ? statsParams.getFormat()
                : "simple";

        log.debug("统计参数: showZeroCounts={}, format={}", showZeroCounts, format);

        // Calculate statistics
        MessageStats stats = calculateStatistics(messageContent);

        // Format output
        String result = formatStatistics(stats, format, showZeroCounts);

        log.debug("消息统计完成: textChars={}, cqCodeTypes={}, format={}",
                stats.getTextCharCount(), stats.getCqCodeCounts().size(), format);

        return result;
    }

    @Override
    protected Class<?> getParamClass() {
        return StatisticsParams.class;
    }

    /**
     * Calculate message statistics
     *
     * @param message Original message content with CQ codes
     * @return MessageStats object with counts
     */
    private MessageStats calculateStatistics(String message) {
        MessageStats stats = new MessageStats();
        Map<String, Integer> cqCodeCounts = new HashMap<>();

        // Parse CQ codes and count by type
        Matcher matcher = CQ_CODE_PATTERN.matcher(message);
        while (matcher.find()) {
            String cqType = matcher.group(1);
            cqCodeCounts.merge(cqType, 1, Integer::sum);
        }

        // Remove all CQ codes to get pure text
        String pureText = message.replaceAll("\\[CQ:[^\\]]+\\]", "");

        // Count text characters (by character, not bytes)
        // FR-003: Each character (Chinese, English, digit, symbol) counts as 1
        stats.setTextCharCount(pureText.length());
        stats.setCqCodeCounts(cqCodeCounts);

        return stats;
    }

    /**
     * Format statistics for output
     *
     * @param stats MessageStats object
     * @param format Output format (simple, detailed, json)
     * @param showZeroCounts Whether to show zero counts
     * @return Formatted statistics string
     */
    private String formatStatistics(MessageStats stats, String format, boolean showZeroCounts) {
        switch (format.toLowerCase()) {
            case "json":
                return formatAsJson(stats, showZeroCounts);
            case "detailed":
                return formatAsDetailed(stats, showZeroCounts);
            case "simple":
            default:
                return formatAsSimple(stats, showZeroCounts);
        }
    }

    /**
     * Format as simple text (FR-004: only non-zero counts)
     * Example: "文字: 5字, 表情: 2个, 图片: 1张"
     */
    private String formatAsSimple(MessageStats stats, boolean showZeroCounts) {
        StringBuilder sb = new StringBuilder();

        // Add text count
        if (stats.getTextCharCount() > 0 || showZeroCounts) {
            sb.append("文字: ").append(stats.getTextCharCount()).append("字");
        }

        // Add CQ code counts
        for (Map.Entry<String, Integer> entry : stats.getCqCodeCounts().entrySet()) {
            if (entry.getValue() > 0 || showZeroCounts) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }

                String type = entry.getKey();
                String label = CQ_TYPE_LABELS.getOrDefault(type, type);
                String unit = CQ_TYPE_UNITS.getOrDefault(type, "个");

                sb.append(label).append(": ").append(entry.getValue()).append(unit);
            }
        }

        return sb.length() > 0 ? sb.toString() : "消息为空";
    }

    /**
     * Format as detailed text with additional information
     */
    private String formatAsDetailed(MessageStats stats, boolean showZeroCounts) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 消息统计\n");
        sb.append("━━━━━━━━━━━━━━\n");

        // Text statistics
        sb.append("📝 文字: ").append(stats.getTextCharCount()).append("字\n");

        // CQ code statistics
        if (!stats.getCqCodeCounts().isEmpty()) {
            sb.append("\n🎨 多媒体内容:\n");
            for (Map.Entry<String, Integer> entry : stats.getCqCodeCounts().entrySet()) {
                if (entry.getValue() > 0 || showZeroCounts) {
                    String type = entry.getKey();
                    String label = CQ_TYPE_LABELS.getOrDefault(type, type);
                    String unit = CQ_TYPE_UNITS.getOrDefault(type, "个");

                    sb.append("  • ").append(label).append(": ")
                      .append(entry.getValue()).append(unit).append("\n");
                }
            }
        }

        // Total count
        int totalCqCodes = stats.getCqCodeCounts().values().stream()
            .mapToInt(Integer::intValue).sum();
        sb.append("\n总计: ").append(stats.getTextCharCount()).append("字 + ")
          .append(totalCqCodes).append("个多媒体元素");

        return sb.toString();
    }

    /**
     * Format as JSON
     */
    private String formatAsJson(MessageStats stats, boolean showZeroCounts) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"textCharCount\": ").append(stats.getTextCharCount()).append(",\n");
        sb.append("  \"cqCodeCounts\": {\n");

        boolean first = true;
        for (Map.Entry<String, Integer> entry : stats.getCqCodeCounts().entrySet()) {
            if (entry.getValue() > 0 || showZeroCounts) {
                if (!first) {
                    sb.append(",\n");
                }
                sb.append("    \"").append(entry.getKey()).append("\": ")
                  .append(entry.getValue());
                first = false;
            }
        }

        sb.append("\n  }\n");
        sb.append("}");

        return sb.toString();
    }

    /**
     * Statistics parameters class
     */
    @Data
    public static class StatisticsParams {
        @JsonProperty("showZeroCounts")
        private Boolean showZeroCounts;

        @JsonProperty("format")
        private String format;
    }

    /**
     * Message statistics data class
     */
    private static class MessageStats {
        private int textCharCount;
        private Map<String, Integer> cqCodeCounts;

        public MessageStats() {
            this.textCharCount = 0;
            this.cqCodeCounts = new HashMap<>();
        }

        public int getTextCharCount() {
            return textCharCount;
        }

        public void setTextCharCount(int textCharCount) {
            this.textCharCount = textCharCount;
        }

        public Map<String, Integer> getCqCodeCounts() {
            return cqCodeCounts;
        }

        public void setCqCodeCounts(Map<String, Integer> cqCodeCounts) {
            this.cqCodeCounts = cqCodeCounts;
        }
    }
}
