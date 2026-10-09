package com.lectureprocessor.llm;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.lectureprocessor.config.AppProperties;
import com.lectureprocessor.util.VectorMath;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;

/**
 * Real local LLM integration backed by Ollama through Spring AI.
 *
 * <p>Models are constructed explicitly from configuration rather than relying on
 * auto-configuration, so the application starts (and tests run) even when Ollama
 * is not running.
 */
@Service
public class OllamaLlmProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(OllamaLlmProvider.class);

    private final AppProperties properties;
    private final OllamaApi ollamaApi;
    private final OllamaChatModel chatModel;
    private final OllamaEmbeddingModel embeddingModel;
    private final RestClient healthClient;

    public OllamaLlmProvider(AppProperties properties) {
        this.properties = properties;
        AppProperties.Ollama ollama = properties.ollama();
        this.ollamaApi = OllamaApi.builder().baseUrl(ollama.baseUrl()).build();
        this.chatModel = OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .options(OllamaChatOptions.builder()
                        .model(ollama.chatModel())
                        .numCtx(4096)
                        .build())
                .build();
        this.embeddingModel = OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .options(OllamaEmbeddingOptions.builder()
                        .model(ollama.embeddingModel())
                        .build())
                .build();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.healthClient = RestClient.builder()
                .baseUrl(ollama.baseUrl())
                .requestFactory(factory)
                .build();
    }

    @Override
    public String chat(String systemPrompt, String userPrompt, LlmOptions options) {
        try {
            ChatClient client = ChatClient.create(chatModel);
            String content = client.prompt()
                    .options(OllamaChatOptions.builder()
                            .model(options.model())
                            .temperature(options.temperature())
                            .numCtx(options.numCtx())
                            .numPredict(options.maxTokens())
                            .seed(options.seed()))
                    .system(systemPrompt == null ? "" : systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
            if (content == null) {
                throw new LlmException("Ollama вернула пустой ответ");
            }
            return sanitize(content);
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException("Ошибка обращения к Ollama (" + options.model() + "): " + e.getMessage(), e);
        }
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        try {
            return embeddingModel.embed(texts).stream().map(VectorMath::normalize).toList();
        } catch (Exception e) {
            throw new LlmException("Ошибка вычисления эмбеддингов: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            healthClient.get().uri("/api/tags").retrieve().toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.debug("Ollama not available at {}: {}", properties.ollama().baseUrl(), e.getMessage());
            return false;
        }
    }

    @Override
    public String defaultChatModel() {
        return properties.ollama().chatModel();
    }

    /** Removes Qwen3 thinking blocks if the model emits them. */
    static String sanitize(String content) {
        return content.replaceAll("(?s) thinking.*?<｜end▁of▁thinking｜>", "").strip();
    }
}
