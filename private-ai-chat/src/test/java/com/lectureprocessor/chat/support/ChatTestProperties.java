package com.lectureprocessor.chat.support;

import java.time.Duration;

import com.lectureprocessor.chat.config.ChatProperties;

public final class ChatTestProperties {

    private ChatTestProperties() {
    }

    public static ChatProperties defaults() {
        return new ChatProperties(
                new ChatProperties.Ollama("http://localhost:11434", "qwen3:1.7b", Duration.ofSeconds(5)),
                new ChatProperties.Security(""),
                new ChatProperties.Limits(5, 100, 4096, 64, 6, 2));
    }

    public static ChatProperties withApiKey(String key) {
        ChatProperties base = defaults();
        return new ChatProperties(base.ollama(), new ChatProperties.Security(key), base.limits());
    }
}
