package com.bonsay.ai.llm;

import java.util.List;
import java.util.Objects;

/**
 * Provider-neutral completion request.
 *
 * @param model       model id; {@code null} lets the client use its default
 * @param system      system prompt (may be empty)
 * @param messages    conversation, oldest first
 * @param maxTokens   output token cap
 * @param temperature sampling temperature
 */
public record LlmRequest(String model, String system, List<ChatMessage> messages, int maxTokens, double temperature) {

    public LlmRequest {
        Objects.requireNonNull(messages, "messages");
        messages = List.copyOf(messages);
        system = system == null ? "" : system;
        if (maxTokens <= 0) throw new IllegalArgumentException("maxTokens must be > 0");
    }

    public static LlmRequest of(String system, String userMessage) {
        return new LlmRequest(null, system, List.of(ChatMessage.user(userMessage)), 1024, 0.0);
    }

    public LlmRequest withModel(String newModel) {
        return new LlmRequest(newModel, system, messages, maxTokens, temperature);
    }

    /** Text of the most recent user turn - handy for fakes and routing heuristics. */
    public String lastUserMessage() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equals(messages.get(i).role())) return messages.get(i).content();
        }
        return "";
    }
}
