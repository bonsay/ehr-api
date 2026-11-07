package com.bonsay.ai.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Transport-agnostic MCP (Model Context Protocol) server core implementing JSON-RPC 2.0.
 *
 * <p>Supported methods: {@code initialize}, {@code notifications/initialized}, {@code ping},
 * {@code tools/list}, {@code tools/call}, {@code resources/list}, {@code resources/read}.</p>
 *
 * <p>Tool failures are returned as a successful JSON-RPC result with {@code isError: true}
 * (per the MCP spec) so the model can see the error and self-correct; protocol failures use
 * JSON-RPC error objects.</p>
 */
public class McpServer {

    public static final String PROTOCOL_VERSION = "2025-06-18";

    static final int PARSE_ERROR = -32700;
    static final int INVALID_REQUEST = -32600;
    static final int METHOD_NOT_FOUND = -32601;
    static final int INVALID_PARAMS = -32602;
    static final int INTERNAL_ERROR = -32603;

    private final ObjectMapper mapper = new ObjectMapper();
    private final String name;
    private final String version;
    private final Map<String, McpTool> tools = new LinkedHashMap<>();
    private final Map<String, McpResource> resources = new LinkedHashMap<>();

    public McpServer(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public void registerTool(McpTool tool) { tools.put(tool.name(), tool); }

    public void registerResource(McpResource resource) { resources.put(resource.uri(), resource); }

    /** Handles one raw JSON-RPC message. Returns empty for notifications (no response expected). */
    public Optional<String> handle(String rawMessage) {
        JsonNode msg;
        try {
            msg = mapper.readTree(rawMessage);
        } catch (Exception e) {
            return Optional.of(error(null, PARSE_ERROR, "Parse error"));
        }
        if (msg == null || !msg.isObject() || !"2.0".equals(msg.path("jsonrpc").asText())
                || !msg.path("method").isTextual()) {
            return Optional.of(error(msg == null ? null : msg.get("id"), INVALID_REQUEST, "Invalid request"));
        }

        JsonNode id = msg.get("id");
        boolean isNotification = id == null || id.isNull();
        String method = msg.get("method").asText();
        JsonNode params = msg.path("params");

        try {
            JsonNode result = switch (method) {
                case "initialize" -> initialize(params);
                case "ping" -> mapper.createObjectNode();
                case "tools/list" -> listTools();
                case "tools/call" -> callTool(params);
                case "resources/list" -> listResources();
                case "resources/read" -> readResource(params);
                default -> {
                    if (method.startsWith("notifications/")) yield null;
                    throw new RpcException(METHOD_NOT_FOUND, "Method not found: " + method);
                }
            };
            if (isNotification) return Optional.empty();
            ObjectNode response = mapper.createObjectNode().put("jsonrpc", "2.0");
            response.set("id", id);
            response.set("result", result == null ? mapper.createObjectNode() : result);
            return Optional.of(response.toString());
        } catch (RpcException e) {
            return isNotification ? Optional.empty() : Optional.of(error(id, e.code, e.getMessage()));
        } catch (RuntimeException e) {
            System.err.println("[mcp] internal error: " + e);
            return isNotification ? Optional.empty() : Optional.of(error(id, INTERNAL_ERROR, "Internal error"));
        }
    }

    private JsonNode initialize(JsonNode params) {
        ObjectNode result = mapper.createObjectNode();
        // Echo the client's version if it is one we accept, otherwise offer ours.
        String requested = params.path("protocolVersion").asText(PROTOCOL_VERSION);
        result.put("protocolVersion", requested.compareTo("2024-11-05") >= 0 ? requested : PROTOCOL_VERSION);
        ObjectNode caps = result.putObject("capabilities");
        caps.putObject("tools").put("listChanged", false);
        caps.putObject("resources").put("listChanged", false).put("subscribe", false);
        result.putObject("serverInfo").put("name", name).put("version", version);
        result.put("instructions", "Personal task manager backed by SQLite. Use list_tasks before "
                + "modifying tasks; run_readonly_query accepts a single SELECT statement only.");
        return result;
    }

    private JsonNode listTools() {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode arr = result.putArray("tools");
        for (McpTool t : tools.values()) {
            ObjectNode node = arr.addObject();
            node.put("name", t.name());
            node.put("description", t.description());
            node.set("inputSchema", t.inputSchema());
            if (t.readOnly()) node.putObject("annotations").put("readOnlyHint", true);
        }
        return result;
    }

    private JsonNode callTool(JsonNode params) {
        String toolName = params.path("name").asText(null);
        if (toolName == null) throw new RpcException(INVALID_PARAMS, "Missing tool name");
        McpTool tool = tools.get(toolName);
        if (tool == null) throw new RpcException(INVALID_PARAMS, "Unknown tool: " + toolName);

        JsonNode args = params.path("arguments");
        if (args.isMissingNode() || args.isNull()) args = mapper.createObjectNode();

        ObjectNode result = mapper.createObjectNode();
        ArrayNode content = result.putArray("content");
        try {
            SchemaValidator.validate(tool.inputSchema(), args);
            String text = tool.call(args);
            content.addObject().put("type", "text").put("text", text);
            result.put("isError", false);
        } catch (ToolException | IllegalArgumentException e) {
            content.addObject().put("type", "text").put("text", "Error: " + e.getMessage());
            result.put("isError", true);
        }
        return result;
    }

    private JsonNode listResources() {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode arr = result.putArray("resources");
        for (McpResource r : resources.values()) {
            arr.addObject().put("uri", r.uri()).put("name", r.name())
                    .put("description", r.description()).put("mimeType", r.mimeType());
        }
        return result;
    }

    private JsonNode readResource(JsonNode params) {
        String uri = params.path("uri").asText(null);
        McpResource r = uri == null ? null : resources.get(uri);
        if (r == null) throw new RpcException(INVALID_PARAMS, "Unknown resource: " + uri);
        ObjectNode result = mapper.createObjectNode();
        result.putArray("contents").addObject().put("uri", uri).put("mimeType", r.mimeType()).put("text", r.read());
        return result;
    }

    private String error(JsonNode id, int code, String message) {
        ObjectNode response = mapper.createObjectNode().put("jsonrpc", "2.0");
        response.set("id", id == null ? mapper.nullNode() : id);
        response.putObject("error").put("code", code).put("message", message);
        return response.toString();
    }

    private static final class RpcException extends RuntimeException {
        final int code;

        RpcException(int code, String message) {
            super(message);
            this.code = code;
        }
    }
}
