package com.bonsay.ai.mcp;

import com.fasterxml.jackson.databind.JsonNode;

/** A callable capability advertised to the model via {@code tools/list}. */
public interface McpTool {
    String name();

    String description();

    /** JSON Schema (draft 2020-12 subset) describing the arguments object. */
    JsonNode inputSchema();

    /** True if the tool never mutates state - surfaced to clients as {@code readOnlyHint}. */
    default boolean readOnly() { return false; }

    /** Executes the tool; return value is sent to the model as text content. */
    String call(JsonNode arguments) throws ToolException;
}
