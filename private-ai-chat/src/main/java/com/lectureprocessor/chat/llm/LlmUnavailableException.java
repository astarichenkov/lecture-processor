package com.lectureprocessor.chat.llm;

/**
 * Raised when the local model backend cannot be reached.
 */
public class LlmUnavailableException extends RuntimeException {

    public LlmUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public LlmUnavailableException(String message) {
        super(message);
    }
}
