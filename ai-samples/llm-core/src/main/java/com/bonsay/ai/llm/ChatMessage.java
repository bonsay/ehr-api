package com.bonsay.ai.llm;

/** A single conversation turn. {@code role} is "user" or "assistant". */
public record ChatMessage(String role, String content) {
    public static ChatMessage user(String content) { return new ChatMessage("user", content); }
    public static ChatMessage assistant(String content) { return new ChatMessage("assistant", content); }
}
