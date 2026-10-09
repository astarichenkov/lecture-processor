package com.lectureprocessor.rag;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lectureprocessor.config.AppProperties;
import com.lectureprocessor.domain.LectureChunk;
import com.lectureprocessor.domain.TranscriptSegment;
import com.lectureprocessor.llm.LlmProvider;
import com.lectureprocessor.repository.ChunkRepository;
import com.lectureprocessor.text.SegmentChunk;
import com.lectureprocessor.text.TextChunker;

/**
 * Builds a persistent local vector index: transcript is chunked with timestamps,
 * chunks are embedded through the local embedding model and stored in SQLite.
 */
@Service
public class EmbeddingIndexService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingIndexService.class);
    private static final int EMBED_BATCH = 16;

    private final TextChunker chunker;
    private final LlmProvider llm;
    private final ChunkRepository chunkRepository;
    private final AppProperties properties;

    public EmbeddingIndexService(TextChunker chunker, LlmProvider llm, ChunkRepository chunkRepository,
            AppProperties properties) {
        this.chunker = chunker;
        this.llm = llm;
        this.chunkRepository = chunkRepository;
        this.properties = properties;
    }

    public int index(String lectureId, List<TranscriptSegment> segments) {
        if (segments == null || segments.isEmpty()) {
            chunkRepository.replaceAll(lectureId, List.of());
            return 0;
        }
        int maxChars = properties.processing().llmChunkChars();
        int overlap = properties.processing().llmChunkOverlapChars();
        List<SegmentChunk> parts = chunker.chunkSegments(segments, maxChars, overlap);

        List<LectureChunk> chunks = new ArrayList<>();
        for (int start = 0; start < parts.size(); start += EMBED_BATCH) {
            List<SegmentChunk> batch = parts.subList(start, Math.min(start + EMBED_BATCH, parts.size()));
            List<float[]> vectors = llm.embed(batch.stream().map(SegmentChunk::text).toList());
            for (int i = 0; i < batch.size(); i++) {
                SegmentChunk part = batch.get(i);
                chunks.add(new LectureChunk(0, lectureId, part.index(), part.startMs(), part.endMs(),
                        part.text(), vectors.get(i)));
            }
        }
        chunkRepository.replaceAll(lectureId, chunks);
        log.info("Indexed {} chunks for lecture {}", chunks.size(), lectureId);
        return chunks.size();
    }

    public boolean isIndexed(String lectureId) {
        return chunkRepository.countByLecture(lectureId) > 0;
    }
}
