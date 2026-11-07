package com.bonsay.ai.llm;

/** Cheap heuristic (~4 chars/token) used when a provider does not report usage. */
public final class TokenEstimator {
    private TokenEstimator() {}

    public static int estimate(String text) {
        if (text == null || text.isEmpty()) return 0;
        return Math.max(1, (int) Math.ceil(text.length() / 4.0));
    }
}
