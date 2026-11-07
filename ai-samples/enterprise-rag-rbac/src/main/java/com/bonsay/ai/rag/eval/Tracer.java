package com.bonsay.ai.rag.eval;

import java.util.Map;

/** Observability hook - implementations ship runs to LangSmith, OpenTelemetry, a file, etc. */
@FunctionalInterface
public interface Tracer {
    void trace(String name, Map<String, Object> inputs, Object outputs);

    Tracer NOOP = (n, i, o) -> {};
}
