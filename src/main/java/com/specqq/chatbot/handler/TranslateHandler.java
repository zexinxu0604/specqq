package com.specqq.chatbot.handler;

import com.specqq.chatbot.dto.MessageReceiveDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Translate Handler
 *
 * <p>T077: Translates text between languages (mock implementation)</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Slf4j
@Component
@HandlerMetadata(
        handlerType = "TRANSLATE",
        name = "Translate Handler",
        description = "Translates text between different languages",
        category = "Language Tools",
        params = {
                @HandlerParam(
                        name = "sourceLang",
                        displayName = "Source Language",
                        type = "enum",
                        required = false,
                        defaultValue = "auto",
                        enumValues = {"auto", "zh", "en", "ja", "ko", "fr", "de", "es"},
                        description = "Source language (auto-detect if not specified)"
                ),
                @HandlerParam(
                        name = "targetLang",
                        displayName = "Target Language",
                        type = "enum",
                        required = true,
                        enumValues = {"zh", "en", "ja", "ko", "fr", "de", "es"},
                        description = "Target language for translation"
                )
        }
)
public class TranslateHandler extends BaseHandler {

    /**
     * Language name mapping for display
     */
    private static final Map<String, String> LANGUAGE_NAMES = Map.of(
            "auto", "Auto-detect",
            "zh", "Chinese",
            "en", "English",
            "ja", "Japanese",
            "ko", "Korean",
            "fr", "French",
            "de", "German",
            "es", "Spanish"
    );

    @Override
    protected String process(MessageReceiveDTO message, Object params) {
        // Extract parameters
        String sourceLang = "auto";
        String targetLang = "en";

        if (params instanceof Map) {
            Map<String, Object> paramMap = (Map<String, Object>) params;
            sourceLang = (String) paramMap.getOrDefault("sourceLang", "auto");
            targetLang = (String) paramMap.getOrDefault("targetLang", "en");
        }

        // Validate target language is specified
        if (targetLang == null || targetLang.isEmpty()) {
            throw new IllegalArgumentException("Target language is required");
        }

        String textToTranslate = message.getMessageContent();

        // Validate input
        if (textToTranslate == null || textToTranslate.trim().isEmpty()) {
            throw new IllegalArgumentException("Text to translate cannot be empty");
        }

        log.info("Translating text: sourceLang={}, targetLang={}, text={}",
                sourceLang, targetLang, textToTranslate);

        try {
            // Mock translation (in production, call real translation API)
            String translatedText = mockTranslate(textToTranslate, sourceLang, targetLang);

            // Format response
            String sourceLanguageName = LANGUAGE_NAMES.getOrDefault(sourceLang, sourceLang);
            String targetLanguageName = LANGUAGE_NAMES.getOrDefault(targetLang, targetLang);

            return String.format("翻译结果 (%s → %s):\n%s",
                    sourceLanguageName,
                    targetLanguageName,
                    translatedText);

        } catch (Exception e) {
            log.error("Translation failed", e);
            throw new RuntimeException("Translation failed: " + e.getMessage());
        }
    }

    /**
     * Mock translation implementation
     * In production, this would call a real translation API (e.g., Google Translate, DeepL)
     *
     * @param text       Text to translate
     * @param sourceLang Source language code
     * @param targetLang Target language code
     * @return Translated text
     */
    private String mockTranslate(String text, String sourceLang, String targetLang) {
        // Simple mock: detect common patterns and return mock translations
        if (targetLang.equals("zh")) {
            // Translating to Chinese
            if (text.toLowerCase().contains("hello")) {
                return text.replaceAll("(?i)hello", "你好");
            }
            if (text.toLowerCase().contains("thank you")) {
                return text.replaceAll("(?i)thank you", "谢谢");
            }
            return "[中文翻译] " + text;
        } else if (targetLang.equals("en")) {
            // Translating to English
            if (text.contains("你好")) {
                return text.replace("你好", "Hello");
            }
            if (text.contains("谢谢")) {
                return text.replace("谢谢", "Thank you");
            }
            return "[English translation] " + text;
        } else if (targetLang.equals("ja")) {
            // Translating to Japanese
            return "[日本語訳] " + text;
        } else if (targetLang.equals("ko")) {
            // Translating to Korean
            return "[한국어 번역] " + text;
        } else if (targetLang.equals("fr")) {
            // Translating to French
            return "[Traduction française] " + text;
        } else if (targetLang.equals("de")) {
            // Translating to German
            return "[Deutsche Übersetzung] " + text;
        } else if (targetLang.equals("es")) {
            // Translating to Spanish
            return "[Traducción al español] " + text;
        }

        // Default: return prefixed text
        return String.format("[Translated to %s] %s", targetLang, text);
    }

    /**
     * Example usage in production with real API:
     *
     * <pre>
     * private String callTranslationApi(String text, String sourceLang, String targetLang) {
     *     String url = String.format(
     *         "https://api.translation-service.com/translate?source=%s&target=%s&text=%s",
     *         sourceLang, targetLang, URLEncoder.encode(text, StandardCharsets.UTF_8)
     *     );
     *
     *     RestTemplate restTemplate = new RestTemplate();
     *     TranslationResponse response = restTemplate.getForObject(url, TranslationResponse.class);
     *
     *     if (response == null || response.getTranslatedText() == null) {
     *         throw new RuntimeException("Translation API returned empty response");
     *     }
     *
     *     return response.getTranslatedText();
     * }
     * </pre>
     */
}
