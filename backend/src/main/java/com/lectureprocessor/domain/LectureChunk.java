package com.lectureprocessor.domain;

/**
 * A retrieval unit derived from the transcript, optionally carrying its local
 * embedding vector.
 */
public record LectureChunk(
        long id,
        String lectureId,
        int sequence,
        long startMs,
        long endMs,
        String text,
        float[] embedding) {
}
