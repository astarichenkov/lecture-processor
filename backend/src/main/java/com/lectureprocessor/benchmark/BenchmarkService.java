package com.lectureprocessor.benchmark;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectMapper;
import com.lectureprocessor.config.AppProperties;

/**
 * Loads benchmark configuration, runs it and persists JSON/CSV/Markdown reports.
 */
@Service
public class BenchmarkService {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final BenchmarkRunner runner;
    private final ObjectMapper objectMapper;
    private final AppProperties properties;

    public BenchmarkService(BenchmarkRunner runner, ObjectMapper objectMapper, AppProperties properties) {
        this.runner = runner;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public BenchmarkConfig loadDefaultConfig() {
        try (InputStream in = BenchmarkService.class.getResourceAsStream("/benchmark/benchmark-config.json")) {
            if (in == null) {
                throw new IllegalStateException("benchmark-config.json not found");
            }
            return objectMapper.readValue(in, BenchmarkConfig.class);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось загрузить конфигурацию бенчмарка", e);
        }
    }

    public BenchmarkConfig loadConfig(Path path) {
        try {
            return objectMapper.readValue(path.toFile(), BenchmarkConfig.class);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Не удалось прочитать " + path, e);
        }
    }

    public Path runAndPersist(BenchmarkConfig config, String sourceText) {
        List<BenchmarkResult> results = runner.run(config, sourceText);
        Path dir = Path.of(properties.storage().root(), "benchmarks",
                LocalDateTime.now().format(STAMP)).toAbsolutePath();
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("results.json"),
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(results),
                    StandardCharsets.UTF_8);
            Files.writeString(dir.resolve("results.csv"), toCsv(results), StandardCharsets.UTF_8);
            Files.writeString(dir.resolve("results.md"), toMarkdown(config, results), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось сохранить результаты бенчмарка", e);
        }
        return dir;
    }

    static String toCsv(List<BenchmarkResult> results) {
        StringBuilder csv = new StringBuilder(
                "id,model,promptVersion,contextLength,temperature,inputTokens,outputTokens,totalMs,"
                        + "tokensPerSecond,peakRamBytes,errorCount,outputValid,keyFactsPreserved,keyFactsTotal,status\n");
        for (BenchmarkResult r : results) {
            csv.append(String.join(",",
                    r.configurationId(), r.model(), r.promptVersion(), String.valueOf(r.contextLength()),
                    String.valueOf(r.temperature()), String.valueOf(r.inputTokens()),
                    String.valueOf(r.outputTokens()), String.valueOf(r.totalMs()),
                    String.valueOf(r.tokensPerSecond()), String.valueOf(r.peakRamBytes()),
                    String.valueOf(r.errorCount()), String.valueOf(r.outputValid()),
                    String.valueOf(r.keyFactsPreserved()), String.valueOf(r.keyFactsTotal()),
                    r.status())).append('\n');
        }
        return csv.toString();
    }

    static String toMarkdown(BenchmarkConfig config, List<BenchmarkResult> results) {
        StringBuilder md = new StringBuilder("# Результаты бенчмарка\n\n");
        md.append("| ID | Модель | Контекст | Prompt | Вход, ток | Выход, ток | Время, мс | ток/с | Ключ. факты | Статус |\n");
        md.append("|----|--------|----------|--------|-----------|------------|-----------|-------|-------------|--------|\n");
        for (BenchmarkResult r : results) {
            md.append("| ").append(r.configurationId())
                    .append(" | ").append(r.model())
                    .append(" | ").append(r.contextLength())
                    .append(" | ").append(r.promptVersion())
                    .append(" | ").append(r.inputTokens())
                    .append(" | ").append(r.outputTokens())
                    .append(" | ").append(r.totalMs())
                    .append(" | ").append(r.tokensPerSecond())
                    .append(" | ").append(r.keyFactsPreserved()).append('/').append(r.keyFactsTotal())
                    .append(" | ").append(r.status())
                    .append(" |\n");
        }
        md.append("\n> Токены оценены приблизительно (~4 символа/токен). VRAM из JVM недоступна.\n");
        return md.toString();
    }
}
