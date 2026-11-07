package com.bonsay.ai.guardrail.eval;

import java.util.List;

/**
 * Golden-dataset row. {@code chaos} injects infrastructure faults: none | primary_down |
 * all_down | malformed | slow.
 */
public record EvalCase(String id, String category, String customerId, String input, String expectedOutcome,
                       List<String> expectedTools, List<String> mustContain, List<String> mustNotContain,
                       String chaos) {
}
