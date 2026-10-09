package com.lectureprocessor.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.lectureprocessor.domain.JobStatus;
import com.lectureprocessor.domain.Lecture;

@Repository
public class LectureRepository {

    private final JdbcTemplate jdbc;

    public LectureRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Lecture> MAPPER = (ResultSet rs, int rowNum) -> new Lecture(
            rs.getString("id"),
            rs.getString("original_filename"),
            rs.getString("stored_filename"),
            rs.getLong("size_bytes"),
            rs.getString("content_type"),
            JobStatus.valueOf(rs.getString("status")),
            rs.getString("error_message"),
            rs.getObject("duration_ms") == null ? null : rs.getLong("duration_ms"),
            rs.getInt("clean_transcript") == 1,
            rs.getInt("extract_slides") == 1,
            rs.getString("raw_transcript_path"),
            rs.getString("cleaned_text"),
            rs.getString("notes_markdown"),
            rs.getString("docx_path"),
            Instant.parse(rs.getString("created_at")),
            Instant.parse(rs.getString("updated_at")));

    public void save(Lecture lecture) {
        jdbc.update("""
                INSERT INTO lecture (id, original_filename, stored_filename, size_bytes, content_type,
                        status, error_message, duration_ms, clean_transcript, extract_slides,
                        raw_transcript_path, cleaned_text, notes_markdown, docx_path, created_at, updated_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT(id) DO UPDATE SET
                        status=excluded.status,
                        error_message=excluded.error_message,
                        duration_ms=excluded.duration_ms,
                        raw_transcript_path=excluded.raw_transcript_path,
                        cleaned_text=excluded.cleaned_text,
                        notes_markdown=excluded.notes_markdown,
                        docx_path=excluded.docx_path,
                        updated_at=excluded.updated_at
                """,
                lecture.id(), lecture.originalFilename(), lecture.storedFilename(), lecture.sizeBytes(),
                lecture.contentType(), lecture.status().name(), lecture.errorMessage(),
                lecture.durationMs(), lecture.cleanTranscript() ? 1 : 0, lecture.extractSlides() ? 1 : 0,
                lecture.rawTranscriptPath(), lecture.cleanedText(), lecture.notesMarkdown(),
                lecture.docxPath(), lecture.createdAt().toString(), lecture.updatedAt().toString());
    }

    public Optional<Lecture> findById(String id) {
        List<Lecture> list = jdbc.query("SELECT * FROM lecture WHERE id = ?", MAPPER, id);
        return list.stream().findFirst();
    }

    public List<Lecture> findAll() {
        return jdbc.query("SELECT * FROM lecture ORDER BY created_at DESC", MAPPER);
    }

    public void deleteById(String id) {
        jdbc.update("DELETE FROM lecture WHERE id = ?", id);
    }
}
