package com.lectureprocessor.chat.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.lectureprocessor.chat.support.ChatTestProperties;

class ApiKeyInterceptorTest {

    @Test
    void allowsEverythingWhenKeyNotConfigured() throws Exception {
        ApiKeyInterceptor interceptor = new ApiKeyInterceptor(ChatTestProperties.defaults());
        assertThat(interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object()))
                .isTrue();
    }

    @Test
    void rejectsMissingKey() throws Exception {
        ApiKeyInterceptor interceptor = new ApiKeyInterceptor(ChatTestProperties.withApiKey("secret"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(new MockHttpServletRequest(), response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).doesNotContain("secret");
    }

    @Test
    void acceptsHeaderKey() throws Exception {
        ApiKeyInterceptor interceptor = new ApiKeyInterceptor(ChatTestProperties.withApiKey("secret"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-API-Key", "secret");
        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
    }

    @Test
    void acceptsBearerKey() throws Exception {
        ApiKeyInterceptor interceptor = new ApiKeyInterceptor(ChatTestProperties.withApiKey("secret"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer secret");
        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
    }
}
