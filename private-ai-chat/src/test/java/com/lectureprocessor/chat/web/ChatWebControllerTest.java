package com.lectureprocessor.chat.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;

import com.lectureprocessor.chat.config.ChatProperties;
import com.lectureprocessor.chat.ratelimit.RateLimiter;
import com.lectureprocessor.chat.security.ClientIdResolver;
import com.lectureprocessor.chat.service.ChatService;

class ChatWebControllerTest {

    private ChatService chatService;
    private RateLimiter rateLimiter;
    private ChatWebController controller;

    private static ChatProperties props(int rateLimit) {
        return new ChatProperties(
                new ChatProperties.Ollama("http://localhost:11434", "qwen3:1.7b", Duration.ofSeconds(5), false),
                new ChatProperties.Security("", false),
                new ChatProperties.Limits(rateLimit, 2000, 4096, 512, 8, 10));
    }

    @BeforeEach
    void setUp() {
        chatService = mock(ChatService.class);
        when(chatService.ask(anyString(), anyString())).thenReturn("ok");
        rateLimiter = new RateLimiter(props(1));
        controller = new ChatWebController(chatService, rateLimiter, new ClientIdResolver(props(1)));
    }

    private static MockHttpServletRequest requestFrom(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        return request;
    }

    @Test
    void rateLimitIsPerClientNotSharedAcrossUiUsers() {
        ExtendedModelMap first = new ExtendedModelMap();
        controller.ask("s", "hi", first, requestFrom("10.0.0.1"));
        assertThat(first.get("error")).isNull();

        // same client, second request within the window -> limited
        ExtendedModelMap second = new ExtendedModelMap();
        controller.ask("s", "hi", second, requestFrom("10.0.0.1"));
        assertThat(second.get("error")).isNotNull();

        // a different client is unaffected
        ExtendedModelMap other = new ExtendedModelMap();
        controller.ask("s", "hi", other, requestFrom("10.0.0.2"));
        assertThat(other.get("error")).isNull();
    }
}
