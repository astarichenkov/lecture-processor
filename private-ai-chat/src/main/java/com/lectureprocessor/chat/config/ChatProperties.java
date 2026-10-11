package com.lectureprocessor.chat.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the private AI chat service. Authentication and limits are
 * supplied via environment variables — never hardcoded.
 */
@ConfigurationProperties(prefix = "app")
public record ChatProperties(
        Ollama ollama,
        Security security,
        Limits limits) {

    /**
     * @param think whether the model should emit its reasoning trace. Disabled
     *              by default because on CPU-only hosts Qwen3 thinking roughly
     *              doubles (or worse) the response time and the trace is not
     *              shown to the user.
     */
    public record Ollama(String baseUrl, String model, Duration timeout, Boolean think) {
    }

    public record Security(String apiKey, boolean trustForwardedFor) {
    }

    public record Limits(
            int rateLimitPerMinute,
            int maxMessageChars,
            int maxContextTokens,
            int maxOutputTokens,
            int maxHistoryMessages,
            int queueCapacity) {
    }
}
