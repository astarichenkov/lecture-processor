package com.lectureprocessor.text;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Loads versioned prompt templates from the classpath. Templates are separate
 * from code so prompt changes are reviewable and benchmarkable.
 */
@Component
public class PromptTemplates {

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String get(String name) {
        return cache.computeIfAbsent(name, key -> {
            String resource = "/prompts/" + key + ".txt";
            try (InputStream in = PromptTemplates.class.getResourceAsStream(resource)) {
                if (in == null) {
                    throw new IllegalArgumentException("Prompt template not found: " + resource);
                }
                return new String(in.readAllBytes(), StandardCharsets.UTF_8).strip();
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot read prompt template: " + resource, e);
            }
        });
    }

    /** Currently shipped prompt versions, used by the benchmark module. */
    public static final String CLEAN_TRANSCRIPT = "clean-transcript-v1";
    public static final String STRUCTURED_NOTES = "structured-notes-v1";
    public static final String SUMMARY = "summary-v1";
    public static final String RAG_ANSWER = "rag-answer-v1";

    /** Day 29 optimized prompt versions (more constrained, lower verbosity). */
    public static final String CLEAN_TRANSCRIPT_V2 = "clean-transcript-v2";
    public static final String STRUCTURED_NOTES_V2 = "structured-notes-v2";
}
