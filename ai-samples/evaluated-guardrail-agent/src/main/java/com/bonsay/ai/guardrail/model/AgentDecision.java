package com.bonsay.ai.guardrail.model;

import com.fasterxml.jackson.databind.JsonNode;

/** Parsed, validated model intent. */
public sealed interface AgentDecision {
    record ToolCall(String tool, JsonNode args) implements AgentDecision {}

    record Final(String answer) implements AgentDecision {}

    record Escalate(String reason) implements AgentDecision {}
}
