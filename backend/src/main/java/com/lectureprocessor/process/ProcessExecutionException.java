package com.lectureprocessor.process;

/**
 * Raised when an external tool cannot be executed or exits unsuccessfully.
 */
public class ProcessExecutionException extends RuntimeException {

    public ProcessExecutionException(String message) {
        super(message);
    }

    public ProcessExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
