package com.lectureprocessor.benchmark;

/**
 * One benchmark configuration (model + context + prompt).
 */
public record BenchmarkConfiguration(
        String id,
        String model,
        String quantization,
        int numCtx,
        double temperature,
        int maxTokens,
        String promptTemplate,
        String description) {
}
