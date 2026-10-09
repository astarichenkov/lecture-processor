package com.lectureprocessor.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Central application configuration. All machine-specific values (binary paths,
 * storage locations, limits) live here and are supplied via application.yml or
 * environment variables — never hardcoded in business logic.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Tools tools,
        Storage storage,
        Upload upload,
        Processing processing,
        Ollama ollama,
        Slides slides,
        Chat chat) {

    public record Tools(
            String ffmpegPath,
            String ffprobePath,
            String whisperPath,
            String whisperModelPath,
            String whisperLanguage,
            Duration timeout) {
    }

    public record Storage(String root) {
    }

    public record Upload(long maxFileSizeBytes) {
    }

    public record Processing(
            int workerThreads,
            int queueCapacity,
            int llmChunkChars,
            int llmChunkOverlapChars,
            int maxChunksPerLecture) {
    }

    public record Ollama(
            String baseUrl,
            String chatModel,
            String embeddingModel,
            int embeddingDimensions,
            Duration timeout) {
    }

    public record Slides(
            double intervalSeconds,
            int maxImages,
            int hashDistanceThreshold) {
    }

    public record Chat(
            int maxHistoryMessages,
            int maxQuestionChars,
            int retrievalTopK,
            double minSimilarity) {
    }
}
