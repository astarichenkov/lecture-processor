package com.lectureprocessor.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.lectureprocessor.domain.LectureChunk;
import com.lectureprocessor.domain.TranscriptSegment;
import com.lectureprocessor.repository.ChunkRepository;
import com.lectureprocessor.support.FakeLlmProvider;
import com.lectureprocessor.support.TestProperties;
import com.lectureprocessor.text.TextChunker;

class EmbeddingIndexServiceTest {

    private final ChunkRepository chunkRepository = Mockito.mock(ChunkRepository.class);
    private final FakeLlmProvider llm = new FakeLlmProvider();
    private final EmbeddingIndexService service = new EmbeddingIndexService(
            new TextChunker(), llm, chunkRepository, TestProperties.defaults("./target/test-data"));

    @Test
    void embedsAndStoresChunksWithTimeRanges() {
        List<TranscriptSegment> segments = List.of(
                new TranscriptSegment(0, 0, 1000, "Вектор — направленный отрезок."),
                new TranscriptSegment(1, 1000, 2000, "Скалярное произведение двух векторов."));
        when(chunkRepository.countByLecture("L1")).thenReturn(0);

        int count = service.index("L1", segments);

        assertThat(count).isPositive();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LectureChunk>> captor = ArgumentCaptor.forClass(List.class);
        verify(chunkRepository).replaceAll(eq("L1"), captor.capture());
        List<LectureChunk> stored = captor.getValue();
        assertThat(stored).isNotEmpty();
        assertThat(stored.getFirst().embedding()).isNotEmpty();
        assertThat(stored.getFirst().startMs()).isZero();
    }

    @Test
    void emptySegmentsClearIndex() {
        int count = service.index("L2", List.of());
        assertThat(count).isZero();
        verify(chunkRepository).replaceAll(eq("L2"), eq(List.of()));
    }
}
