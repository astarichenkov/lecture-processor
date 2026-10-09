package com.lectureprocessor.job;

/**
 * Raised when the bounded processing queue is full.
 */
public class JobQueueFullException extends RuntimeException {

    public JobQueueFullException(String message) {
        super(message);
    }
}
