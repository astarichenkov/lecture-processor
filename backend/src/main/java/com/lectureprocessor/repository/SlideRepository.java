package com.lectureprocessor.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.lectureprocessor.domain.Slide;

@Repository
public class SlideRepository {

    private final JdbcTemplate jdbc;

    public SlideRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void replaceAll(String lectureId, List<Slide> slides) {
        jdbc.update("DELETE FROM slide WHERE lecture_id = ?", lectureId);
        jdbc.batchUpdate("""
                        INSERT INTO slide (lecture_id, sequence, timestamp_ms, image_path, phash)
                        VALUES (?,?,?,?,?)
                        """,
                slides.stream().map(s -> new Object[] {
                        lectureId, s.sequence(), s.timestampMs(), s.imagePath(), s.phash()
                }).toList());
    }

    public List<Slide> findByLecture(String lectureId) {
        return jdbc.query("""
                        SELECT id, lecture_id, sequence, timestamp_ms, image_path, phash
                        FROM slide WHERE lecture_id = ? ORDER BY sequence
                        """,
                (rs, rowNum) -> new Slide(
                        rs.getLong("id"), rs.getString("lecture_id"), rs.getInt("sequence"),
                        rs.getLong("timestamp_ms"), rs.getString("image_path"), rs.getString("phash")),
                lectureId);
    }
}
