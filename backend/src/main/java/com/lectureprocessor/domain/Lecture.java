package com.lectureprocessor.domain;

import java.time.Instant;

/**
 * A lecture uploaded by the user and its processing state.
 */
public record Lecture(
        String id,
        String originalFilename,
        String storedFilename,
        long sizeBytes,
        String contentType,
        JobStatus status,
        String errorMessage,
        Long durationMs,
        boolean cleanTranscript,
        boolean extractSlides,
        String rawTranscriptPath,
        String cleanedText,
        String notesMarkdown,
        String docxPath,
        Instant createdAt,
        Instant updatedAt) {

    public Lecture withStatus(JobStatus newStatus) {
        return new Lecture(id, originalFilename, storedFilename, sizeBytes, contentType, newStatus,
                status == JobStatus.FAILED ? errorMessage : null, durationMs, cleanTranscript,
                extractSlides, rawTranscriptPath, cleanedText, notesMarkdown, docxPath,
                createdAt, Instant.now());
    }

    public Lecture withError(String message) {
        return new Lecture(id, originalFilename, storedFilename, sizeBytes, contentType,
                JobStatus.FAILED, message, durationMs, cleanTranscript, extractSlides,
                rawTranscriptPath, cleanedText, notesMarkdown, docxPath, createdAt, Instant.now());
    }
}
