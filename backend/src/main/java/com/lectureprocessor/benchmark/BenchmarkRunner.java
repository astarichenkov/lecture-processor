package com.lectureprocessor.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lectureprocessor.llm.LlmOptions;
import com.lectureprocessor.llm.LlmProvider;
import com.lectureprocessor.text.PromptTemplates;

/**
 * Runs reproducible benchmark configurations against the local LLM and records
 * measured metrics plus deterministic quality checks (token estimates, output
 * validity, key-fact preservation). Measurements that require out-of-process
 * tooling (e.g. VRAM) are reported as unknown rather than invented.
 */
@Service
public class BenchmarkRunner {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkRunner.class);

    private final LlmProvider llm;
    private final PromptTemplates prompts;

    public BenchmarkRunner(LlmProvider llm, PromptTemplates prompts) {
        this.llm = llm;
        this.prompts = prompts;
    }

    public List<BenchmarkResult> run(BenchmarkConfig config, String sourceText) {
        List<BenchmarkResult> results = new ArrayList<>();
        if (!llm.isAvailable()) {
            for (BenchmarkConfiguration configuration : config.configurations()) {
                results.add(new BenchmarkResult(configuration.id(), configuration.model(),
                        configuration.quantization(), configuration.promptTemplate(), configuration.numCtx(),
                        configuration.temperature(), 0, 0, 0, 0, 0,
                        "unknown (Ollama unavailable)", 0, false, 0, config.keyFacts().size(),
                        "SKIPPED", "Ollama недоступна — замер не выполнялся", null));
            }
            return results;
        }
        for (BenchmarkConfiguration configuration : config.configurations()) {
            results.add(runOne(configuration, sourceText, config.keyFacts()));
        }
        return results;
    }

    private BenchmarkResult runOne(BenchmarkConfiguration configuration, String sourceText, List<String> keyFacts) {
        String system = prompts.get(configuration.promptTemplate());
        LlmOptions options = new LlmOptions(configuration.model(), configuration.temperature(),
                configuration.maxTokens(), configuration.numCtx(), 42);
        long usedBefore = usedMemory();
        AtomicLong peak = new AtomicLong(usedBefore);
        Thread sampler = new Thread(() -> {
            for (int i = 0; i < 600; i++) {
                peak.accumulateAndGet(usedMemory(), Math::max);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        sampler.setDaemon(true);
        sampler.start();

        long start = System.nanoTime();
        String output = null;
        String status;
        String notes = "";
        int errors = 0;
        try {
            output = llm.chat(system, sourceText, options);
            status = "PASS";
        } catch (Exception e) {
            errors = 1;
            status = "FAIL";
            notes = e.getMessage() == null ? "unknown error" : e.getMessage();
            log.warn("Benchmark {} failed: {}", configuration.id(), notes);
        } finally {
            sampler.interrupt();
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        int inputTokens = estimateTokens(sourceText);
        int outputTokens = output == null ? 0 : estimateTokens(output);
        int preserved = output == null ? 0 : countKeyFacts(output, keyFacts);
        boolean valid = output != null && output.strip().length() >= 40;
        double tokensPerSecond = elapsedMs > 0 && outputTokens > 0
                ? outputTokens / (elapsedMs / 1000.0) : 0;

        return new BenchmarkResult(configuration.id(), configuration.model(), configuration.quantization(),
                configuration.promptTemplate(), configuration.numCtx(), configuration.temperature(),
                inputTokens, outputTokens, elapsedMs, round(tokensPerSecond),
                Math.max(0, peak.get() - usedBefore), "unknown (Ollama runs out-of-process)",
                errors, valid, preserved, keyFacts.size(), status, notes,
                output == null ? null : truncate(output, 4000));
    }

    static int estimateTokens(String text) {
        if (text == null) {
            return 0;
        }
        // Conservative approximation: ~4 characters per token.
        return Math.max(1, text.length() / 4);
    }

    static int countKeyFacts(String output, List<String> keyFacts) {
        if (keyFacts == null || keyFacts.isEmpty()) {
            return 0;
        }
        String lower = output.toLowerCase();
        int count = 0;
        for (String fact : keyFacts) {
            if (fact != null && !fact.isBlank() && lower.contains(fact.toLowerCase())) {
                count++;
            }
        }
        return count;
    }

    private static long usedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }
}
