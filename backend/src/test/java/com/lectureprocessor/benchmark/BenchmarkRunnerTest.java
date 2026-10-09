package com.lectureprocessor.benchmark;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.lectureprocessor.support.FakeLlmProvider;
import com.lectureprocessor.text.PromptTemplates;

class BenchmarkRunnerTest {

    private final FakeLlmProvider llm = new FakeLlmProvider();
    private final BenchmarkRunner runner = new BenchmarkRunner(llm, new PromptTemplates());

    private final BenchmarkConfig config = new BenchmarkConfig(
            "test",
            List.of("вектор", "отрезок"),
            List.of(new BenchmarkConfiguration("A", "qwen3:4b", "Q4", 4096, 0.2, 256,
                    "basic-summary-v1", "test config")));

    @Test
    void recordsMeasuredResults() {
        llm.setChatFunction((system, user) -> "Вектор — это направленный отрезок, обладающий длиной и направлением.");

        List<BenchmarkResult> results = runner.run(config, "Вектор это направленный отрезок.");

        assertThat(results).hasSize(1);
        BenchmarkResult result = results.getFirst();
        assertThat(result.status()).isEqualTo("PASS");
        assertThat(result.outputValid()).isTrue();
        assertThat(result.inputTokens()).isPositive();
        assertThat(result.outputTokens()).isPositive();
        assertThat(result.keyFactsPreserved()).isEqualTo(2);
        assertThat(result.vramNote()).contains("unknown");
    }

    @Test
    void marksSkippedWhenLlmUnavailable() {
        llm.setAvailable(false);
        List<BenchmarkResult> results = runner.run(config, "текст");
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().status()).isEqualTo("SKIPPED");
        assertThat(results.getFirst().notes()).contains("Ollama");
    }

    @Test
    void marksFailedWhenLlmThrows() {
        llm.setChatFunction((system, user) -> {
            throw new IllegalStateException("boom");
        });
        List<BenchmarkResult> results = runner.run(config, "текст");
        assertThat(results.getFirst().status()).isEqualTo("FAIL");
        assertThat(results.getFirst().errorCount()).isEqualTo(1);
    }
}
