package com.lectureprocessor.chat.support;

import java.util.List;
import java.util.function.Function;

import com.lectureprocessor.chat.llm.ChatTurn;
import com.lectureprocessor.chat.llm.LlmClient;

public class FakeLlmClient implements LlmClient {

    private boolean available = true;
    private Function<String, String> responder = message -> "echo: " + message;
    private volatile int callCount;

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public void setResponder(Function<String, String> responder) {
        this.responder = responder;
    }

    public int callCount() {
        return callCount;
    }

    @Override
    public String generate(String systemPrompt, List<ChatTurn> history, String userMessage,
            int maxOutputTokens, int maxContextTokens) {
        callCount++;
        return responder.apply(userMessage);
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public String modelName() {
        return "fake-chat-model";
    }
}
