package com.lectureprocessor.llm;

import java.util.List;

/**
 * Provider abstraction for local LLM chat and embeddings. Business logic only
 * depends on this interface, so the concrete backend (Spring AI/Ollama now,
 * something else later) can be replaced without rewrites.
 */
public interface LlmProvider {

    /** Single-turn generation. */
    String chat(String systemPrompt, String userPrompt, LlmOptions options);

    /** Embeds a batch of texts, returning L2-normalized vectors. */
    List<float[]> embed(List<String> texts);

    /** Cheap reachability check used for health reporting. */
    boolean isAvailable();

    /** Default chat model configured for this deployment. */
    String defaultChatModel();
}
