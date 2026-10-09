package com.lectureprocessor.benchmark;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

class BenchmarkServiceTest {

    private final BenchmarkService service = new BenchmarkService(null, new ObjectMapper(), null);

    @Test
    void loadsDefaultConfigurationWithFourVariants() {
        BenchmarkConfig config = service.loadDefaultConfig();
        assertThat(config.configurations()).hasSize(4);
        assertThat(config.configurations()).extracting(BenchmarkConfiguration::id)
                .containsExactly("A-qwen3-4b-4096-basic", "B-qwen3-4b-4096-notes",
                        "C-qwen3-4b-8192-notes", "D-qwen3-8b-4096-notes");
    }

    @Test
    void serializesCsvAndMarkdown() {
        BenchmarkResult result = new BenchmarkResult("A", "qwen3:4b", "Q4", "basic-summary-v1", 4096, 0.2,
                100, 50, 1234, 40.5, 1024, "unknown", 0, true, 2, 3, "PASS", "", "out");
        String csv = BenchmarkService.toCsv(List.of(result));
        String md = BenchmarkService.toMarkdown(new BenchmarkConfig("t", List.of(), List.of()), List.of(result));

        assertThat(csv).contains("A,qwen3:4b").contains("PASS");
        assertThat(md).contains("| A |").contains("PASS");
    }
}
