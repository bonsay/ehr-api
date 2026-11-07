package com.bonsay.ai.rag.guardrails;

import java.util.List;

/** Outcome of a guardrail check. {@code text} is the (possibly sanitised) content. */
public record GuardrailResult(boolean allowed, String text, List<String> violations) {
    public static GuardrailResult pass(String text) { return new GuardrailResult(true, text, List.of()); }
}
