package com.lectureprocessor.chat.web;

import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.lectureprocessor.chat.llm.LlmUnavailableException;
import com.lectureprocessor.chat.ratelimit.RateLimitExceededException;

/**
 * Converts chat exceptions into clean HTTP responses. Stack traces are never
 * returned to clients.
 */
@RestControllerAdvice(assignableTypes = ChatApiController.class)
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> rateLimited(RateLimitExceededException e) {
        return response(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    }

    @ExceptionHandler(InvalidChatRequestException.class)
    public ResponseEntity<Map<String, Object>> invalid(InvalidChatRequestException e) {
        return response(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(ServiceBusyException.class)
    public ResponseEntity<Map<String, Object>> busy(ServiceBusyException e) {
        return response(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    }

    @ExceptionHandler(LlmUnavailableException.class)
    public ResponseEntity<Map<String, Object>> unavailable(LlmUnavailableException e) {
        log.warn("LLM unavailable: {}", e.getMessage());
        return response(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    private static ResponseEntity<Map<String, Object>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message == null ? "" : message));
    }
}
