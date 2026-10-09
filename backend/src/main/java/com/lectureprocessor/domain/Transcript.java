package com.lectureprocessor.domain;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Full transcription result. The raw model output is preserved separately from
 * any post-processed text.
 *
 * @param segments parsed timestamped segments
 * @param rawJson  untouched JSON emitted by the transcription tool
 */
public record Transcript(List<TranscriptSegment> segments, String rawJson) {

    public Transcript {
        segments = List.copyOf(segments);
    }

    public boolean isEmpty() {
        return segments.isEmpty();
    }

    /** Plain text without timestamps, used as LLM input. */
    public String plainText() {
        return segments.stream()
                .map(TranscriptSegment::text)
                .collect(Collectors.joining(" "))
                .trim();
    }

    /** Text with [hh:mm:ss] prefixes, useful for human-readable exports. */
    public String timestampedText() {
        return segments.stream()
                .map(s -> "[" + formatTimestamp(s.startMs()) + "] " + s.text())
                .collect(Collectors.joining("\n"));
    }

    public static String formatTimestamp(long ms) {
        long totalSeconds = ms / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
