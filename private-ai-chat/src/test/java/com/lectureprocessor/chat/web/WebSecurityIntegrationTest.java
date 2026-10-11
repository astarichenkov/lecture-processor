package com.lectureprocessor.chat.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.lectureprocessor.chat.llm.LlmClient;
import com.lectureprocessor.chat.support.FakeLlmClient;

/**
 * End-to-end (in-process) regression test for the Day 30 security boundary:
 * the machine API requires the key, while the browser UI must remain usable
 * when a key is configured.
 */
@SpringBootTest(properties = {
        "app.security.api-key=test-secret",
        "app.limits.rate-limit-per-minute=100"
})
class WebSecurityIntegrationTest {

    @TestConfiguration
    static class TestLlmConfig {
        @Bean
        @Primary
        LlmClient testLlmClient() {
            return new FakeLlmClient();
        }
    }

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void healthIsOpenWithoutKey() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void apiChatWithoutKeyIsRejected() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiChatWithWrongKeyIsRejected() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .header("X-API-Key", "wrong")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiChatWithCorrectKeySucceeds() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .header("X-API-Key", "test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("echo: hi"));
    }

    @Test
    void browserUiIsNotBlockedByApiKey() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(post("/chat")
                        .param("session", "s1")
                        .param("message", "hi"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("echo: hi")));
    }
}
