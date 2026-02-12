package com.specqq.chatbot.handler;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.specqq.chatbot.dto.MessageReceiveDTO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Weather Handler
 *
 * <p>天气查询处理器（模拟实现）</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Slf4j
@Component
@HandlerMetadata(
    handlerType = "WEATHER",
    name = "天气查询",
    description = "查询指定城市的天气信息（模拟实现）",
    category = "信息查询",
    params = {
        @HandlerParam(
            name = "city",
            displayName = "城市",
            type = "string",
            required = true,
            description = "要查询天气的城市名称"
        ),
        @HandlerParam(
            name = "apiKey",
            displayName = "API密钥",
            type = "string",
            required = false,
            description = "天气API的密钥（可选）"
        )
    }
)
public class WeatherHandler extends BaseHandler {

    @Override
    protected String process(MessageReceiveDTO message, Object params) {
        WeatherParams weatherParams = extractWeatherParams(params);

        if (weatherParams == null || weatherParams.getCity() == null || weatherParams.getCity().isEmpty()) {
            return "请提供城市名称，例如：北京、上海、深圳";
        }

        String city = weatherParams.getCity();

        // 模拟天气查询（实际应该调用真实的天气API）
        String weather = getMockWeather(city);

        log.debug("Weather Handler 处理消息: city={}, weather={}", city, weather);

        return weather;
    }

    @Override
    protected Class<?> getParamClass() {
        return WeatherParams.class;
    }

    /**
     * 提取天气参数
     */
    private WeatherParams extractWeatherParams(Object params) {
        if (params == null) {
            return null;
        }

        if (params instanceof WeatherParams) {
            return (WeatherParams) params;
        }

        if (params instanceof String) {
            try {
                return objectMapper.readValue((String) params, WeatherParams.class);
            } catch (Exception e) {
                log.warn("无法解析 Weather 参数: {}", e.getMessage());
                return null;
            }
        }

        return null;
    }

    /**
     * 模拟天气查询
     */
    private String getMockWeather(String city) {
        // 模拟数据
        return String.format("【%s天气】\n" +
                "今天：晴转多云，15-25℃\n" +
                "明天：小雨，12-20℃\n" +
                "空气质量：良\n" +
                "温馨提示：早晚温差较大，注意增减衣物", city);
    }

    /**
     * Weather 参数类
     */
    @Data
    public static class WeatherParams {
        @JsonProperty("city")
        private String city;

        @JsonProperty("apiKey")
        private String apiKey;
    }
}
