package com.lectureprocessor.text;

/**
 * Raised when LLM text processing produces an invalid or empty result.
 */
public class ProcessingException extends RuntimeException {

    public ProcessingException(String message) {
        super(message);
    }
}
