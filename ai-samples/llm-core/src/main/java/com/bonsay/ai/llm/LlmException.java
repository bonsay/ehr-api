package com.bonsay.ai.llm;

/** Raised when a completion fails. {@link #isRetryable()} drives retry/fallback decisions. */
public class LlmException extends RuntimeException {
    private final boolean retryable;
    private final int statusCode;

    public LlmException(String message, boolean retryable, int statusCode, Throwable cause) {
        super(message, cause);
        this.retryable = retryable;
        this.statusCode = statusCode;
    }

    public LlmException(String message, boolean retryable) {
        this(message, retryable, -1, null);
    }

    public boolean isRetryable() { return retryable; }

    public int statusCode() { return statusCode; }
}
