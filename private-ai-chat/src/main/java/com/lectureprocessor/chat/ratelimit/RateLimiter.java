package com.lectureprocessor.chat.ratelimit;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.lectureprocessor.chat.config.ChatProperties;

/**
 * Fixed-window per-client rate limiter (default 5 requests / 60 s). Keeps the
 * data in memory; suitable for a single-instance private service.
 */
@Component
public class RateLimiter {

    private static final long WINDOW_MS = 60_000;

    private final int limit;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public RateLimiter(ChatProperties properties) {
        this.limit = properties.limits().rateLimitPerMinute();
    }

    public boolean tryAcquire(String clientId) {
        long now = System.currentTimeMillis();
        Deque<Long> window = hits.computeIfAbsent(clientId, key -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && now - window.peekFirst() >= WINDOW_MS) {
                window.pollFirst();
            }
            if (window.size() >= limit) {
                return false;
            }
            window.addLast(now);
            return true;
        }
    }
}
