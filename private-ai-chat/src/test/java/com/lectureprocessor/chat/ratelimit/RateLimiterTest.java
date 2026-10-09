package com.lectureprocessor.chat.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.lectureprocessor.chat.support.ChatTestProperties;

class RateLimiterTest {

    private final RateLimiter limiter = new RateLimiter(ChatTestProperties.defaults());

    @Test
    void allowsUpToLimitThenRejects() {
        for (int i = 0; i < 5; i++) {
            assertThat(limiter.tryAcquire("client-1")).as("request %d", i).isTrue();
        }
        assertThat(limiter.tryAcquire("client-1")).isFalse();
    }

    @Test
    void tracksClientsIndependently() {
        for (int i = 0; i < 5; i++) {
            limiter.tryAcquire("a");
        }
        assertThat(limiter.tryAcquire("a")).isFalse();
        assertThat(limiter.tryAcquire("b")).isTrue();
    }
}
