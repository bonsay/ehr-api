package com.bonsay.ai.llm;

/** The only abstraction the sample projects depend on, so any provider or fake can be plugged in. */
@FunctionalInterface
public interface LlmClient {
    LlmResponse complete(LlmRequest request) throws LlmException;
}
