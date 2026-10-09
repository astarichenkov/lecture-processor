package com.lectureprocessor.benchmark;

/**
 * Measured outcome of a single benchmark configuration run. Fields that could
 * not be measured are explicitly marked with {@link #status} SKIPPED and a
 * reason in {@link #notes} — never fabricated.
 */
public record BenchmarkResult(
        String configurationId,
        String model,
        String quantization,
        String promptVersion,
        int contextLength,
        double temperature,
        int inputTokens,
        int outputTokens,
        long totalMs,
        double tokensPerSecond,
        long peakRamBytes,
        String vramNote,
        int errorCount,
        boolean outputValid,
        int keyFactsPreserved,
        int keyFactsTotal,
        String status,
        String notes,
        String output) {
}
