package com.lectureprocessor.llm;

/**
 * Per-request LLM generation options. Kept independent from any vendor SDK so
 * callers and benchmarks can vary model/context without touching provider code.
 */
public record LlmOptions(
        String model,
        Double temperature,
        Integer maxTokens,
        Integer numCtx,
        Integer seed) {

    public static LlmOptions of(String model) {
        return new LlmOptions(model, 0.2, 1024, 4096, 42);
    }

    public LlmOptions withModel(String newModel) {
        return new LlmOptions(newModel, temperature, maxTokens, numCtx, seed);
    }

    public LlmOptions withNumCtx(int newNumCtx) {
        return new LlmOptions(model, temperature, maxTokens, newNumCtx, seed);
    }

    public LlmOptions withTemperature(double newTemperature) {
        return new LlmOptions(model, newTemperature, maxTokens, numCtx, seed);
    }

    public LlmOptions withMaxTokens(int newMaxTokens) {
        return new LlmOptions(model, temperature, newMaxTokens, numCtx, seed);
    }
}
