package com.lectureprocessor.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.lectureprocessor.domain.LectureChunk;
import com.lectureprocessor.repository.ChunkRepository;
import com.lectureprocessor.support.FakeLlmProvider;
import com.lectureprocessor.support.TestProperties;

class RetrievalServiceTest {

    private final ChunkRepository chunkRepository = Mockito.mock(ChunkRepository.class);
    private final FakeLlmProvider llm = new FakeLlmProvider();
    private final RetrievalService service = new RetrievalService(chunkRepository, llm,
            TestProperties.defaults("./target/test-data"));

    @Test
    void returnsChunksOrderedBySimilarity() {
        float[] query = FakeLlmProvider.embedOne("вектор");
        float[] opposite = new float[query.length];
        for (int i = 0; i < query.length; i++) {
            opposite[i] = -query[i];
        }
        LectureChunk relevant = new LectureChunk(1, "L1", 0, 0, 1000,
                "вектор направленный отрезок", query.clone());
        LectureChunk unrelated = new LectureChunk(2, "L1", 1, 1000, 2000,
                "совершенно другой текст про музыку", opposite);
        when(chunkRepository.findByLecture("L1")).thenReturn(List.of(unrelated, relevant));

        List<ScoredChunk> result = service.retrieve("L1", "вектор", 5, 0.0);

        assertThat(result).isNotEmpty();
        assertThat(result.getFirst().chunk().id()).isEqualTo(1);
    }

    @Test
    void returnsEmptyWhenNoChunks() {
        when(chunkRepository.findByLecture("L2")).thenReturn(List.of());
        assertThat(service.retrieve("L2", "вопрос", 5, 0.0)).isEmpty();
    }
}
