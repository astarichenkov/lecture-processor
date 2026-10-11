package com.lectureprocessor.chat.security;

import org.springframework.stereotype.Component;

import com.lectureprocessor.chat.config.ChatProperties;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the per-client key used for rate limiting.
 *
 * <p>{@code X-Forwarded-For} is only honoured when {@code
 * app.security.trust-forwarded-for=true}, i.e. when the service really sits
 * behind a trusted reverse proxy. Otherwise a client could spoof the header
 * and bypass the rate limit.
 */
@Component
public class ClientIdResolver {

    private final boolean trustForwardedFor;

    public ClientIdResolver(ChatProperties properties) {
        this.trustForwardedFor = properties.security().trustForwardedFor();
    }

    public String resolve(HttpServletRequest request) {
        if (trustForwardedFor) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].strip();
            }
        }
        return request.getRemoteAddr();
    }
}
