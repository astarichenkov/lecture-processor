package com.lectureprocessor.rag;

import com.lectureprocessor.domain.LectureChunk;

/**
 * A retrieved chunk and its cosine similarity to the query.
 */
public record ScoredChunk(LectureChunk chunk, double score) {
}
