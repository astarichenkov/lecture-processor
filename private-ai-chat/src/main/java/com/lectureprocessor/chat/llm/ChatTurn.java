package com.lectureprocessor.chat.llm;

/**
 * A single conversation turn.
 */
public record ChatTurn(String role, String content) {

    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";
}
