package com.lectureprocessor.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.lectureprocessor.chat.config.ChatProperties;
import com.lectureprocessor.chat.llm.ChatTurn;
import com.lectureprocessor.chat.llm.LlmUnavailableException;
import com.lectureprocessor.chat.memory.ChatMemoryStore;
import com.lectureprocessor.chat.support.FakeLlmClient;
import com.lectureprocessor.chat.web.InvalidChatRequestException;

class ChatServiceTest {

    private final FakeLlmClient llm = new FakeLlmClient();

    private ChatService service(ChatProperties props) {
        return new ChatService(props, llm, new ChatMemoryStore(props));
    }

    private static ChatProperties props(int maxContext, int maxOutput, int maxHistory) {
        return new ChatProperties(
                new ChatProperties.Ollama("http://localhost:11434", "qwen3:1.7b", Duration.ofSeconds(5), false),
                new ChatProperties.Security("", false),
                new ChatProperties.Limits(5, 100, maxContext, maxOutput, maxHistory, 2));
    }

    @Test
    void answersAndStoresHistory() {
        ChatService service = service(props(4096, 64, 6));
        String answer = service.ask("s1", "Привет");

        assertThat(answer).isEqualTo("echo: Привет");
        assertThat(llm.callCount()).isEqualTo(1);
    }

    @Test
    void rejectsBlankAndOversizedMessages() {
        ChatService service = service(props(4096, 64, 6));
        assertThatThrownBy(() -> service.ask("s1", "  "))
                .isInstanceOf(InvalidChatRequestException.class);
        assertThatThrownBy(() -> service.ask("s1", "x".repeat(101)))
                .isInstanceOf(InvalidChatRequestException.class)
                .hasMessageContaining("длинное");
    }

    @Test
    void propagatesLlmFailure() {
        llm.setResponder(message -> {
            throw new LlmUnavailableException("down");
        });
        ChatService service = service(props(4096, 64, 6));
        assertThatThrownBy(() -> service.ask("s1", "hi")).isInstanceOf(LlmUnavailableException.class);
    }

    @Test
    void failsFastWhenLlmUnavailable() {
        llm.setAvailable(false);
        ChatService service = service(props(4096, 64, 6));
        assertThatThrownBy(() -> service.ask("s1", "hi"))
                .isInstanceOf(LlmUnavailableException.class)
                .hasMessageContaining("недоступна");
        assertThat(llm.callCount()).isZero();
    }

    @Test
    void trimsHistoryToContextBudget() {
        ChatService service = service(props(200, 5, 50));
        List<ChatTurn> history = List.of(
                new ChatTurn(ChatTurn.ROLE_USER, "a".repeat(400)),
                new ChatTurn(ChatTurn.ROLE_ASSISTANT, "b".repeat(400)));

        List<ChatTurn> trimmed = service.trimToContext(history, "hi");

        assertThat(trimmed.size()).isLessThan(history.size());
    }

    @Test
    void queueIsBounded() {
        ChatService service = service(props(4096, 64, 6));
        assertThat(service.queueSize()).isZero();
    }
}
