package com.lectureprocessor.chat.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.lectureprocessor.chat.config.ChatProperties;

class ClientIdResolverTest {

    private static ChatProperties props(boolean trustForwardedFor) {
        return new ChatProperties(
                new ChatProperties.Ollama("http://localhost:11434", "qwen3:1.7b", Duration.ofSeconds(5), false),
                new ChatProperties.Security("", trustForwardedFor),
                new ChatProperties.Limits(5, 2000, 4096, 512, 8, 10));
    }

    @Test
    void ignoresSpoofedForwardedHeaderByDefault() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "1.2.3.4");

        assertThat(new ClientIdResolver(props(false)).resolve(request)).isEqualTo("10.0.0.1");
    }

    @Test
    void honoursForwardedHeaderBehindTrustedProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "1.2.3.4, 10.0.0.1");

        assertThat(new ClientIdResolver(props(true)).resolve(request)).isEqualTo("1.2.3.4");
    }
}
