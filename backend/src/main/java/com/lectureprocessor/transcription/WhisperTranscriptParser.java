package com.lectureprocessor.transcription;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.lectureprocessor.domain.Transcript;
import com.lectureprocessor.domain.TranscriptSegment;

/**
 * Parses the JSON produced by whisper.cpp ({@code whisper-cli -oj}).
 *
 * <p>Different whisper.cpp builds emit slightly different shapes; this parser
 * accepts a {@code transcription} or {@code segments} array and reads millisecond
 * {@code offsets} when present, otherwise parses {@code HH:MM:SS,mmm} timestamps.
 */
@Component
public class WhisperTranscriptParser {

    private final ObjectMapper objectMapper;

    public WhisperTranscriptParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Transcript parse(String json) {
        if (json == null || json.isBlank()) {
            throw new TranscriptionException("Пустой ответ транскрипции");
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode segments = root.has("transcription") ? root.get("transcription") : root.get("segments");
            if (segments == null || !segments.isArray()) {
                throw new TranscriptionException("В JSON транскрипции нет массива сегментов");
            }
            List<TranscriptSegment> result = new ArrayList<>();
            int sequence = 0;
            for (JsonNode node : segments) {
                String text = node.path("text").asText("").strip();
                if (text.isEmpty()) {
                    continue;
                }
                long start = readMillis(node, "from", true);
                long end = readMillis(node, "to", false);
                result.add(new TranscriptSegment(sequence++, start, Math.max(start, end), text));
            }
            return new Transcript(result, json);
        } catch (TranscriptionException e) {
            throw e;
        } catch (Exception e) {
            throw new TranscriptionException("Не удалось разобрать JSON транскрипции", e);
        }
    }

    private static long readMillis(JsonNode segment, String key, boolean isStart) {
        JsonNode offsets = segment.path("offsets");
        if (offsets.has(key) && offsets.get(key).isNumber()) {
            return offsets.get(key).asLong();
        }
        JsonNode timestamps = segment.path("timestamps");
        if (timestamps.has(key) && timestamps.get(key).isTextual()) {
            return parseTimestamp(timestamps.get(key).asText());
        }
        return 0;
    }

    static long parseTimestamp(String value) {
        // supports HH:MM:SS,mmm and HH:MM:SS.mmm
        String normalized = value.replace(',', '.');
        String[] parts = normalized.split(":");
        try {
            if (parts.length == 3) {
                long hours = Long.parseLong(parts[0]);
                long minutes = Long.parseLong(parts[1]);
                double seconds = Double.parseDouble(parts[2]);
                return hours * 3_600_000 + minutes * 60_000 + Math.round(seconds * 1000);
            }
            if (parts.length == 2) {
                long minutes = Long.parseLong(parts[0]);
                double seconds = Double.parseDouble(parts[1]);
                return minutes * 60_000 + Math.round(seconds * 1000);
            }
            return Math.round(Double.parseDouble(normalized) * 1000);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
