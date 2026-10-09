package com.lectureprocessor.domain;

import java.time.Instant;

/**
 * A single message in a per-lecture chat session.
 */
public record ChatMessage(
        long id,
        String lectureId,
        String sessionId,
        String role,
        String content,
        String sourcesJson,
        Instant createdAt) {

    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";
}
