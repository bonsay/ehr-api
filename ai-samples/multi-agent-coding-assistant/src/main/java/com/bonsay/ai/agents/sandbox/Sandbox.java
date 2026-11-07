package com.bonsay.ai.agents.sandbox;

import java.util.Map;

/** Compiles the given sources and runs {@code mainClass} in isolation. */
public interface Sandbox {
    ExecutionResult run(Map<String, String> sourcesByClassName, String mainClass);
}
