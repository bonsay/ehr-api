package com.bonsay.ai.llm;

/** Provider-neutral completion result including token usage for cost accounting. */
public record LlmResponse(String text, String model, int inputTokens, int outputTokens, String stopReason) {

    public static LlmResponse of(String text, String model) {
        return new LlmResponse(text, model, TokenEstimator.estimate(text), TokenEstimator.estimate(text), "end_turn");
    }
}
