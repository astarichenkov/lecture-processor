package com.lectureprocessor.llm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Guards the generation budget. Regression: with a small maxTokens on qwen3,
 * the reasoning tokens consumed the whole budget and the final answer came back
 * empty or truncated.
 */
class LlmOptionsTest {

    @Test
    void defaultBudgetLeavesRoomAfterReasoning() {
        LlmOptions options = LlmOptions.of("qwen3:4b");
        assertThat(options.maxTokens()).isGreaterThanOrEqualTo(2048);
        assertThat(options.numCtx()).isGreaterThan(options.maxTokens());
    }

    @Test
    void derivedOptionsKeepBudget() {
        LlmOptions options = LlmOptions.of("qwen3:4b").withModel("qwen3:8b").withNumCtx(8192);
        assertThat(options.model()).isEqualTo("qwen3:8b");
        assertThat(options.numCtx()).isEqualTo(8192);
        assertThat(options.maxTokens()).isGreaterThanOrEqualTo(2048);
    }
}
