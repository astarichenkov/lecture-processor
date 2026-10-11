package com.lectureprocessor.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.lectureprocessor.chat.security.ApiKeyInterceptor;

/**
 * Registers the API-key interceptor for the machine API (health stays open).
 *
 * <p>The browser UI under {@code /} and {@code /chat} is intentionally not
 * intercepted: a browser cannot attach an API key to the HTMX POST, and the
 * service is only reachable on the private LAN. The programmatic API at
 * {@code /api/chat} remains authenticated.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ApiKeyInterceptor apiKeyInterceptor;

    public WebConfig(ApiKeyInterceptor apiKeyInterceptor) {
        this.apiKeyInterceptor = apiKeyInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiKeyInterceptor).addPathPatterns("/api/chat");
    }
}
