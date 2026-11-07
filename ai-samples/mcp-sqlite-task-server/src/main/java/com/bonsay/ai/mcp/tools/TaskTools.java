package com.bonsay.ai.mcp.tools;

import com.bonsay.ai.mcp.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.function.Function;

/** Registers every task-manager tool and resource on an {@link McpServer}. */
public final class TaskTools {

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private TaskTools() {}

    public static void registerAll(McpServer server, TaskRepository repo) {
        server.registerTool(tool("create_task", "Create a new task. Returns the created task.", false, """
                {"type":"object","additionalProperties":false,
                 "properties":{
                   "title":{"type":"string","minLength":1,"maxLength":200},
                   "description":{"type":"string","maxLength":2000},
                   "priority":{"type":"string","enum":["low","medium","high"]},
                   "due_date":{"type":"string","description":"ISO date YYYY-MM-DD"}},
                 "required":["title"]}""",
                a -> json(repo.create(a.get("title").asText(), text(a, "description"),
                        text(a, "priority"), text(a, "due_date")))));

        server.registerTool(tool("list_tasks", "List tasks ordered by priority then due date, optionally filtered.", true, """
                {"type":"object","additionalProperties":false,
                 "properties":{
                   "status":{"type":"string","enum":["open","in_progress","done"]},
                   "priority":{"type":"string","enum":["low","medium","high"]},
                   "limit":{"type":"integer","minimum":1,"maximum":200}}}""",
                a -> json(repo.list(text(a, "status"), text(a, "priority"), a.path("limit").asInt(50)))));

        server.registerTool(tool("search_tasks", "Full-text search over task titles and descriptions.", true, """
                {"type":"object","additionalProperties":false,
                 "properties":{"query":{"type":"string","minLength":1,"maxLength":200},
                               "limit":{"type":"integer","minimum":1,"maximum":200}},
                 "required":["query"]}""",
                a -> json(repo.search(a.get("query").asText(), a.path("limit").asInt(20)))));

        server.registerTool(tool("update_task_status", "Move a task to open, in_progress or done.", false, """
                {"type":"object","additionalProperties":false,
                 "properties":{"id":{"type":"integer","minimum":1},
                               "status":{"type":"string","enum":["open","in_progress","done"]}},
                 "required":["id","status"]}""",
                a -> json(repo.updateStatus(a.get("id").asLong(), a.get("status").asText()))));

        server.registerTool(tool("delete_task",
                "Permanently delete a task. Requires confirm=true; ask the user before calling.", false, """
                {"type":"object","additionalProperties":false,
                 "properties":{"id":{"type":"integer","minimum":1},"confirm":{"type":"boolean"}},
                 "required":["id","confirm"]}""",
                a -> {
                    if (!a.get("confirm").asBoolean()) throw new ToolException("deletion not confirmed");
                    if (!repo.delete(a.get("id").asLong())) throw new ToolException("task not found");
                    return "deleted task " + a.get("id").asLong();
                }));

        server.registerTool(tool("task_stats", "Counts of open, in-progress, done and overdue tasks.", true,
                "{\"type\":\"object\",\"additionalProperties\":false,\"properties\":{}}",
                a -> json(repo.stats())));

        server.registerTool(tool("run_readonly_query",
                "Run ONE read-only SQL SELECT against the tasks database (max " + TaskRepository.MAX_ROWS
                        + " rows). Read the tasks://schema resource first.", true, """
                {"type":"object","additionalProperties":false,
                 "properties":{"sql":{"type":"string","minLength":1,"maxLength":2000}},
                 "required":["sql"]}""",
                a -> json(repo.readOnlyQuery(a.get("sql").asText()))));

        server.registerResource(new McpResource("tasks://schema", "Database schema",
                "DDL of the SQLite task database", "text/plain", repo::schema));
        server.registerResource(new McpResource("tasks://open", "Open tasks",
                "All tasks that are not done, highest priority first", "application/json",
                () -> json(repo.list("open", null, 200))));
    }

    private static McpTool tool(String name, String description, boolean readOnly, String schema,
                                Function<JsonNode, String> fn) {
        JsonNode parsed;
        try {
            parsed = MAPPER.readTree(schema);
        } catch (Exception e) {
            throw new IllegalStateException("bad schema for " + name, e);
        }
        return new McpTool() {
            public String name() { return name; }
            public String description() { return description; }
            public JsonNode inputSchema() { return parsed; }
            public boolean readOnly() { return readOnly; }
            public String call(JsonNode arguments) { return fn.apply(arguments); }
        };
    }

    private static String text(JsonNode a, String field) {
        JsonNode n = a.get(field);
        return n == null || n.isNull() ? null : n.asText();
    }

    private static String json(Object o) {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (Exception e) {
            throw new ToolException("serialisation failed", e);
        }
    }
}
