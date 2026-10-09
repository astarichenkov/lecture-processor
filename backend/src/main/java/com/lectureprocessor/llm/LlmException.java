package com.lectureprocessor.llm;

/**
 * Raised when the LLM backend is unreachable or returns an error.
 */
public class LlmException extends RuntimeException {

    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
