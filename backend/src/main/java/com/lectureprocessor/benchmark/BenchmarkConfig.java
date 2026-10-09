package com.lectureprocessor.benchmark;

import java.util.List;

/**
 * Top-level benchmark definition, loadable from JSON.
 */
public record BenchmarkConfig(
        String sourceName,
        List<String> keyFacts,
        List<BenchmarkConfiguration> configurations) {
}
