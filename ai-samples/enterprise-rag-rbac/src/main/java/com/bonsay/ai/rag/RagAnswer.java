package com.bonsay.ai.rag;

import java.util.List;

/** Response returned to the chat client. Contexts are included for evaluation, not shown to users. */
public record RagAnswer(String answer, List<String> citations, List<String> contexts,
                        List<String> guardrailEvents, boolean blocked, long latencyMs) {
}
