package com.lectureprocessor.chat.web;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lectureprocessor.chat.ratelimit.RateLimiter;
import com.lectureprocessor.chat.service.ChatService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * HTTP API for the private AI chat: health + chat.
 */
@RestController
@RequestMapping("/api")
public class ChatApiController {

    private final ChatService chatService;
    private final RateLimiter rateLimiter;

    public ChatApiController(ChatService chatService, RateLimiter rateLimiter) {
        this.chatService = chatService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "model", chatService.modelName(),
                "ollamaAvailable", chatService.llmAvailable());
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request, HttpServletRequest http) {
        if (!rateLimiter.tryAcquire(clientId(http))) {
            throw new com.lectureprocessor.chat.ratelimit.RateLimitExceededException();
        }
        String sessionId = request.sessionId() == null || request.sessionId().isBlank()
                ? UUID.randomUUID().toString()
                : request.sessionId();
        String answer = chatService.ask(sessionId, request.message());
        return ResponseEntity.ok(new ChatResponse(sessionId, answer));
    }

    static String clientId(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].strip();
        }
        return request.getRemoteAddr();
    }

    public record ChatRequest(String sessionId, String message) {
    }

    public record ChatResponse(String sessionId, String answer) {
    }

    @GetMapping("/chat/ping")
    public Map<String, Object> ping() {
        return Map.of("status", HttpStatus.OK.value(), "queued", chatService.queueSize());
    }
}
