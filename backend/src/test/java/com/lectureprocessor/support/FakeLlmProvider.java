package com.lectureprocessor.support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import com.lectureprocessor.llm.LlmOptions;
import com.lectureprocessor.llm.LlmProvider;
import com.lectureprocessor.util.VectorMath;

/**
 * Deterministic in-memory {@link LlmProvider} for tests. Chat responses are
 * configurable; embeddings are bag-of-words vectors so retrieval behaves
 * predictably without any external service.
 */
public class FakeLlmProvider implements LlmProvider {

    private static final int DIMENSIONS = 32;

    private boolean available = true;
    private BiFunction<String, String, String> chatFunction =
            (system, user) -> "ANSWER: " + user;
    private final List<String> chatCalls = new ArrayList<>();

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public void setChatFunction(BiFunction<String, String, String> chatFunction) {
        this.chatFunction = chatFunction;
    }

    public List<String> chatCalls() {
        return chatCalls;
    }

    @Override
    public String chat(String systemPrompt, String userPrompt, LlmOptions options) {
        chatCalls.add(userPrompt);
        return chatFunction.apply(systemPrompt, userPrompt);
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        return texts.stream().map(FakeLlmProvider::embedOne).toList();
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public String defaultChatModel() {
        return "fake-model";
    }

    public static float[] embedOne(String text) {
        float[] vector = new float[DIMENSIONS];
        for (String word : text.toLowerCase().split("[^\\p{L}\\p{N}]+")) {
            if (!word.isBlank()) {
                vector[Math.floorMod(word.hashCode(), DIMENSIONS)] += 1f;
            }
        }
        return VectorMath.normalize(vector);
    }
}
