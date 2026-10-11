package com.lectureprocessor.chat.web;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.lectureprocessor.chat.llm.LlmUnavailableException;
import com.lectureprocessor.chat.ratelimit.RateLimiter;
import com.lectureprocessor.chat.security.ClientIdResolver;
import com.lectureprocessor.chat.service.ChatService;
import com.lectureprocessor.chat.support.ChatTestProperties;

class ChatApiControllerTest {

    private ChatService chatService;
    private RateLimiter rateLimiter;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        chatService = mock(ChatService.class);
        rateLimiter = new RateLimiter(ChatTestProperties.defaults());
        ChatApiController controller = new ChatApiController(chatService, rateLimiter,
                new ClientIdResolver(ChatTestProperties.defaults()));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void healthReportsModel() throws Exception {
        when(chatService.modelName()).thenReturn("qwen3:1.7b");
        when(chatService.llmAvailable()).thenReturn(true);

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.model").value("qwen3:1.7b"))
                .andExpect(jsonPath("$.ollamaAvailable").value(true));
    }

    @Test
    void chatReturnsAnswer() throws Exception {
        when(chatService.ask(anyString(), anyString())).thenReturn("Привет!");

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"s1\",\"message\":\"Привет\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Привет!"))
                .andExpect(jsonPath("$.sessionId").value("s1"));
    }

    @Test
    void oversizedMessageReturns400() throws Exception {
        when(chatService.ask(anyString(), anyString()))
                .thenThrow(new InvalidChatRequestException("Сообщение слишком длинное"));

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"s1\",\"message\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Сообщение слишком длинное"));
    }

    @Test
    void unavailableLlmReturns503() throws Exception {
        when(chatService.ask(anyString(), anyString()))
                .thenThrow(new LlmUnavailableException("Локальная модель недоступна"));

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"s1\",\"message\":\"hi\"}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void rateLimitReturns429() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"sessionId\":\"s1\",\"message\":\"hi\"}"))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"s1\",\"message\":\"hi\"}"))
                .andExpect(status().isTooManyRequests());
    }
}
