package com.lectureprocessor.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.lectureprocessor.domain.ChatMessage;

@Repository
public class ChatMessageRepository {

    private final JdbcTemplate jdbc;

    public ChatMessageRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void save(String lectureId, String sessionId, String role, String content, String sourcesJson) {
        jdbc.update("""
                        INSERT INTO chat_message (lecture_id, session_id, role, content, sources_json, created_at)
                        VALUES (?,?,?,?,?,?)
                        """,
                lectureId, sessionId, role, content, sourcesJson, Instant.now().toString());
    }

    public List<ChatMessage> findBySession(String lectureId, String sessionId) {
        return jdbc.query("""
                        SELECT id, lecture_id, session_id, role, content, sources_json, created_at
                        FROM chat_message
                        WHERE lecture_id = ? AND session_id = ? ORDER BY id
                        """,
                (rs, rowNum) -> new ChatMessage(
                        rs.getLong("id"), rs.getString("lecture_id"), rs.getString("session_id"),
                        rs.getString("role"), rs.getString("content"), rs.getString("sources_json"),
                        Instant.parse(rs.getString("created_at"))),
                lectureId, sessionId);
    }

    public void deleteBySession(String lectureId, String sessionId) {
        jdbc.update("DELETE FROM chat_message WHERE lecture_id = ? AND session_id = ?", lectureId, sessionId);
    }
}
