package com.lectureprocessor.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.lectureprocessor.domain.LectureChunk;
import com.lectureprocessor.util.EmbeddingCodec;

@Repository
public class ChunkRepository {

    private final JdbcTemplate jdbc;

    public ChunkRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void replaceAll(String lectureId, List<LectureChunk> chunks) {
        jdbc.update("DELETE FROM lecture_chunk WHERE lecture_id = ?", lectureId);
        jdbc.batchUpdate("""
                        INSERT INTO lecture_chunk (lecture_id, sequence, start_ms, end_ms, text, embedding, embedding_dim)
                        VALUES (?,?,?,?,?,?,?)
                        """,
                chunks.stream().map(c -> new Object[] {
                        lectureId, c.sequence(), c.startMs(), c.endMs(), c.text(),
                        EmbeddingCodec.encode(c.embedding()),
                        c.embedding() == null ? null : c.embedding().length
                }).toList());
    }

    public List<LectureChunk> findByLecture(String lectureId) {
        return jdbc.query("""
                        SELECT id, lecture_id, sequence, start_ms, end_ms, text, embedding
                        FROM lecture_chunk WHERE lecture_id = ? ORDER BY sequence
                        """,
                (rs, rowNum) -> new LectureChunk(
                        rs.getLong("id"), rs.getString("lecture_id"), rs.getInt("sequence"),
                        rs.getLong("start_ms"), rs.getLong("end_ms"), rs.getString("text"),
                        EmbeddingCodec.decode(rs.getBytes("embedding"))),
                lectureId);
    }

    public int countByLecture(String lectureId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM lecture_chunk WHERE lecture_id = ?", Integer.class, lectureId);
        return count == null ? 0 : count;
    }

    public void deleteByLecture(String lectureId) {
        jdbc.update("DELETE FROM lecture_chunk WHERE lecture_id = ?", lectureId);
    }
}
