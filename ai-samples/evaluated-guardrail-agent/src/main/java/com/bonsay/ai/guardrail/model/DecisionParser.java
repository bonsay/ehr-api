package com.bonsay.ai.guardrail.model;

import com.bonsay.ai.llm.Json;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

/**
 * Strict parser for the agent's JSON protocol. Anything that does not match is rejected so the
 * agent can ask for a repair (once) and otherwise fall back - never "best-effort" execution.
 * <pre>{"action":"tool","tool":"lookup_order","args":{"order_id":"ORD-1"}}
 * {"action":"final","answer":"..."}
 * {"action":"escalate","reason":"..."}</pre>
 */
public final class DecisionParser {
    private DecisionParser() {}

    public static Optional<AgentDecision> parse(String raw) {
        if (raw == null) return Optional.empty();
        String text = raw.trim();
        if (text.startsWith("```")) text = text.replaceAll("^```(?:json)?\\s*|\\s*```$", "");
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) return Optional.empty();
        JsonNode n;
        try {
            n = Json.read(text.substring(start, end + 1));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        return switch (n.path("action").asText("")) {
            case "tool" -> n.path("tool").isTextual() && n.path("args").isObject()
                    ? Optional.of(new AgentDecision.ToolCall(n.get("tool").asText(), n.get("args")))
                    : Optional.empty();
            case "final" -> n.path("answer").isTextual() && !n.get("answer").asText().isBlank()
                    ? Optional.of(new AgentDecision.Final(n.get("answer").asText()))
                    : Optional.empty();
            case "escalate" -> Optional.of(new AgentDecision.Escalate(n.path("reason").asText("unspecified")));
            default -> Optional.empty();
        };
    }
}
