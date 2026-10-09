package com.lectureprocessor.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.lectureprocessor.domain.TranscriptSegment;

class TextChunkerTest {

    private final TextChunker chunker = new TextChunker();

    @Test
    void groupsSegmentsRespectingMaxCharsAndTimestamps() {
        List<TranscriptSegment> segments = List.of(
                new TranscriptSegment(0, 0, 1000, "aaaa"),
                new TranscriptSegment(1, 1000, 2000, "bbbb"),
                new TranscriptSegment(2, 2000, 3000, "cccc"),
                new TranscriptSegment(3, 3000, 4000, "dddd"));

        List<SegmentChunk> chunks = chunker.chunkSegments(segments, 11, 0);

        assertThat(chunks).hasSizeGreaterThanOrEqualTo(2);
        assertThat(chunks.getFirst().startMs()).isZero();
        assertThat(chunks.getLast().endMs()).isEqualTo(4000);
    }

    @Test
    void carriesOverlapIntoNextChunk() {
        List<TranscriptSegment> segments = List.of(
                new TranscriptSegment(0, 0, 1000, "aaaa"),
                new TranscriptSegment(1, 1000, 2000, "bbbb"),
                new TranscriptSegment(2, 2000, 3000, "cccc"));

        List<SegmentChunk> chunks = chunker.chunkSegments(segments, 6, 5);

        assertThat(chunks.size()).isGreaterThanOrEqualTo(2);
        assertThat(chunks.get(1).text()).contains("bbbb");
    }

    @Test
    void oversizedSingleSegmentBecomesOwnChunk() {
        List<TranscriptSegment> segments = List.of(new TranscriptSegment(0, 0, 1000, "x".repeat(500)));
        List<SegmentChunk> chunks = chunker.chunkSegments(segments, 100, 10);
        assertThat(chunks).hasSize(1);
    }

    @Test
    void chunksPlainText() {
        String text = "Первое предложение. Второе предложение. Третье предложение.";
        List<TextChunk> chunks = chunker.chunkText(text, 25, 5);
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.getFirst().text()).isNotBlank();
        assertThat(chunks.getFirst().index()).isZero();
    }

    @Test
    void rejectsInvalidBounds() {
        assertThatThrownBy(() -> chunker.chunkSegments(List.of(), 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> chunker.chunkSegments(List.of(), 10, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
