package com.lectureprocessor.domain;

/**
 * A concrete source reference attached to a grounded RAG answer. It is always
 * derived from actual retrieval results — never from model output.
 */
public record SourceRef(
        String lectureId,
        String lectureFilename,
        long chunkId,
        int chunkSequence,
        long startMs,
        long endMs,
        String excerpt) {
}
