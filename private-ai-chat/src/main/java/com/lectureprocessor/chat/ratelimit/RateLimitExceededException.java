package com.lectureprocessor.chat.ratelimit;

/**
 * Raised when a client exceeds the configured request rate.
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException() {
        super("Превышен лимит запросов. Попробуйте позже.");
    }
}
