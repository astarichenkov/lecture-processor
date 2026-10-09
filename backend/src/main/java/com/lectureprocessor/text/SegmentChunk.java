package com.lectureprocessor.text;

/**
 * A chunk built from transcript segments, preserving its time span.
 */
public record SegmentChunk(int index, long startMs, long endMs, String text) {
}
