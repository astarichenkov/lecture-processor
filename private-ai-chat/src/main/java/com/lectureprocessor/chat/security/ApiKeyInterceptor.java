package com.lectureprocessor.chat.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.lectureprocessor.chat.config.ChatProperties;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Simple API-key authentication for protected endpoints. When
 * {@code app.security.api-key} is blank the check is disabled (local/dev),
 * otherwise requests must send {@code X-API-Key} or {@code Authorization:
 * Bearer <key>}. Never logs the key itself.
 */
@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    private final String apiKey;

    public ApiKeyInterceptor(ChatProperties properties) {
        this.apiKey = properties.security().apiKey();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (apiKey == null || apiKey.isBlank()) {
            return true;
        }
        String provided = request.getHeader("X-API-Key");
        if (provided == null) {
            String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.startsWith("Bearer ")) {
                provided = authorization.substring(7);
            }
        }
        if (apiKey.equals(provided)) {
            return true;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Требуется корректный API-ключ\"}");
        return false;
    }
}
