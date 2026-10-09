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

    public record Ollama(String baseUrl, String model, Duration timeout) {
    }

    public record Security(String apiKey) {
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
