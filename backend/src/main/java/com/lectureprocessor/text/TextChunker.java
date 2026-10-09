package com.lectureprocessor.text;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.lectureprocessor.domain.TranscriptSegment;

/**
 * Splits long lectures into bounded chunks so no single LLM request becomes
 * unbounded. Chunking is aware of transcript timestamps for RAG and of sentence
 * boundaries for plain text.
 */
@Component
public class TextChunker {

    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[.!?…])\\s+");

    /**
     * Groups transcript segments into chunks of at most {@code maxChars}
     * characters, carrying up to {@code overlapChars} of trailing text into the
     * next chunk. Time spans follow the grouped segments.
     */
    public List<SegmentChunk> chunkSegments(List<TranscriptSegment> segments, int maxChars, int overlapChars) {
        requireValidBounds(maxChars, overlapChars);
        List<SegmentChunk> chunks = new ArrayList<>();
        List<TranscriptSegment> current = new ArrayList<>();
        int currentLength = 0;
        int index = 0;

        for (TranscriptSegment segment : segments) {
            int segmentLength = segment.text().length() + 1;
            if (!current.isEmpty() && currentLength + segmentLength > maxChars) {
                chunks.add(toChunk(index++, current));
                List<TranscriptSegment> carried = overlapTail(current, overlapChars);
                current = new ArrayList<>(carried);
                currentLength = carried.stream().mapToInt(s -> s.text().length() + 1).sum();
            }
            current.add(segment);
            currentLength += segmentLength;
            // A single oversized segment still becomes its own chunk.
            if (current.size() == 1 && currentLength > maxChars) {
                chunks.add(toChunk(index++, current));
                current = new ArrayList<>();
                currentLength = 0;
            }
        }
        if (!current.isEmpty()) {
            chunks.add(toChunk(index, current));
        }
        return chunks;
    }

    /** Splits plain text at sentence boundaries into bounded chunks. */
    public List<TextChunk> chunkText(String text, int maxChars, int overlapChars) {
        requireValidBounds(maxChars, overlapChars);
        List<TextChunk> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }
        List<String> sentences = new ArrayList<>();
        Matcher matcher = SENTENCE_BOUNDARY.matcher(text);
        int last = 0;
        while (matcher.find()) {
            sentences.add(text.substring(last, matcher.start()).strip());
            last = matcher.end();
        }
        sentences.add(text.substring(last).strip());

        StringBuilder buffer = new StringBuilder();
        int chunkStart = 0;
        int index = 0;
        int cursor = 0;
        for (String sentence : sentences) {
            if (sentence.isEmpty()) {
                cursor += sentence.length() + 1;
                continue;
            }
            if (buffer.length() > 0 && buffer.length() + sentence.length() + 1 > maxChars) {
                chunks.add(new TextChunk(index++, buffer.toString().strip(), chunkStart, cursor));
                String overlap = tail(buffer.toString(), overlapChars);
                buffer.setLength(0);
                buffer.append(overlap);
                chunkStart = Math.max(0, cursor - overlap.length());
            }
            if (buffer.length() > 0) {
                buffer.append(' ');
            }
            buffer.append(sentence);
            cursor += sentence.length() + 1;
        }
        if (!buffer.isEmpty()) {
            chunks.add(new TextChunk(index, buffer.toString().strip(), chunkStart, text.length()));
        }
        return chunks;
    }

    private static SegmentChunk toChunk(int index, List<TranscriptSegment> segments) {
        long start = segments.getFirst().startMs();
        long end = segments.getLast().endMs();
        String text = String.join(" ", segments.stream().map(TranscriptSegment::text).toList()).strip();
        return new SegmentChunk(index, start, end, text);
    }

    private static List<TranscriptSegment> overlapTail(List<TranscriptSegment> segments, int overlapChars) {
        if (overlapChars <= 0) {
            return new ArrayList<>();
        }
        List<TranscriptSegment> carried = new ArrayList<>();
        int accumulated = 0;
        for (int i = segments.size() - 1; i >= 0; i--) {
            TranscriptSegment segment = segments.get(i);
            if (accumulated + segment.text().length() > overlapChars && !carried.isEmpty()) {
                break;
            }
            carried.add(0, segment);
            accumulated += segment.text().length() + 1;
        }
        return carried;
    }

    private static String tail(String text, int overlapChars) {
        if (overlapChars <= 0 || text.length() <= overlapChars) {
            return overlapChars <= 0 ? "" : text;
        }
        return text.substring(text.length() - overlapChars);
    }

    private static void requireValidBounds(int maxChars, int overlapChars) {
        if (maxChars <= 0) {
            throw new IllegalArgumentException("maxChars must be positive");
        }
        if (overlapChars < 0 || overlapChars >= maxChars) {
            throw new IllegalArgumentException("overlapChars must be in [0, maxChars)");
        }
    }
}
