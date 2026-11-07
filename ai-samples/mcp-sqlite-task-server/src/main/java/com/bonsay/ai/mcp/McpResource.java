package com.bonsay.ai.mcp;

import java.util.function.Supplier;

/** Read-only context the client can attach to a conversation. */
public record McpResource(String uri, String name, String description, String mimeType, Supplier<String> reader) {
    public String read() { return reader.get(); }
}
