package com.lectureprocessor.domain;

/**
 * A single timestamped transcript segment as produced by Whisper.
 *
 * @param sequence zero-based order within the lecture
 * @param startMs  segment start in milliseconds
 * @param endMs    segment end in milliseconds
 * @param text     recognized text
 */
public record TranscriptSegment(int sequence, long startMs, long endMs, String text) {
}
