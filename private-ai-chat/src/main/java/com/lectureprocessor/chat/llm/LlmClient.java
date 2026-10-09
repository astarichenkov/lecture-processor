package com.lectureprocessor.chat.llm;

import java.util.List;

/**
 * Minimal LLM client abstraction so the chat service can be tested and the
 * backend replaced without changes to business logic.
 */
public interface LlmClient {

    String generate(String systemPrompt, List<ChatTurn> history, String userMessage,
            int maxOutputTokens, int maxContextTokens);

    boolean isAvailable();

    String modelName();
}
