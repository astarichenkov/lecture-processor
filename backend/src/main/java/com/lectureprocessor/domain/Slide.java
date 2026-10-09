package com.lectureprocessor.domain;

/**
 * An extracted presentation frame associated with a lecture timestamp.
 */
public record Slide(
        long id,
        String lectureId,
        int sequence,
        long timestampMs,
        String imagePath,
        String phash) {
}
