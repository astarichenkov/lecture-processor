package com.lectureprocessor.chat.llm;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.lectureprocessor.chat.config.ChatProperties;

/**
 * Ollama-backed LLM client using Spring AI. Models are built explicitly so the
 * service starts even when Ollama is down; availability is checked lazily.
 */
@Service
public class OllamaLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaLlmClient.class);

    private final ChatProperties properties;
    private final OllamaChatModel chatModel;
    private final RestClient healthClient;

    public OllamaLlmClient(ChatProperties properties) {
        this.properties = properties;
        OllamaApi api = OllamaApi.builder().baseUrl(properties.ollama().baseUrl()).build();
        this.chatModel = OllamaChatModel.builder()
                .ollamaApi(api)
                .options(options(properties.limits().maxOutputTokens(), properties.limits().maxContextTokens()).build())
                .build();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.healthClient = RestClient.builder()
                .baseUrl(properties.ollama().baseUrl())
                .requestFactory(factory)
                .build();
    }

    @Override
    public String generate(String systemPrompt, List<ChatTurn> history, String userMessage,
            int maxOutputTokens, int maxContextTokens) {
        List<Message> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(new SystemMessage(systemPrompt));
        }
        for (ChatTurn turn : history) {
            messages.add(ChatTurn.ROLE_ASSISTANT.equals(turn.role())
                    ? new AssistantMessage(turn.content())
                    : new UserMessage(turn.content()));
        }
        messages.add(new UserMessage(userMessage));

        try {
            String content = ChatClient.create(chatModel).prompt()
                    .options(options(maxOutputTokens, maxContextTokens))
                    .messages(messages)
                    .call()
                    .content();
            if (content == null || content.isBlank()) {
                throw new LlmUnavailableException("Модель вернула пустой ответ");
            }
            return content.strip();
        } catch (LlmUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Ollama generation failed: {}", e.getMessage());
            throw new LlmUnavailableException("Локальная модель недоступна", e);
        }
    }

    /**
     * Builds per-request Ollama options. Thinking is disabled unless explicitly
     * enabled via {@code app.ollama.think=true}; on CPU it costs a lot of time
     * and its trace is never returned to the caller.
     */
    private OllamaChatOptions.Builder options(int maxOutputTokens, int maxContextTokens) {
        OllamaChatOptions.Builder builder = OllamaChatOptions.builder()
                .model(properties.ollama().model())
                .numCtx(maxContextTokens)
                .numPredict(maxOutputTokens)
                .temperature(0.3);
        if (Boolean.TRUE.equals(properties.ollama().think())) {
            builder.enableThinking();
        } else {
            builder.disableThinking();
        }
        return builder;
    }

    @Override
    public boolean isAvailable() {
        try {
            healthClient.get().uri("/api/tags").retrieve().toBodilessEntity();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String modelName() {
        return properties.ollama().model();
    }
}
