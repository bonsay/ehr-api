package com.bonsay.ai.mcp;

import com.bonsay.ai.mcp.tools.TaskTools;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class McpServerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicInteger ids = new AtomicInteger();
    private TaskRepository repo;
    private McpServer server;

    @BeforeEach
    void setUp(@TempDir Path dir) throws Exception {
        repo = new TaskRepository(dir.resolve("tasks.db"));
        server = new McpServer("test", "0.0.1");
        TaskTools.registerAll(server, repo);
    }

    @AfterEach
    void tearDown() throws Exception {
        repo.close();
    }

    private JsonNode rpc(String method, String paramsJson) throws Exception {
        String req = "{\"jsonrpc\":\"2.0\",\"id\":" + ids.incrementAndGet() + ",\"method\":\"" + method
                + "\",\"params\":" + paramsJson + "}";
        return mapper.readTree(server.handle(req).orElseThrow());
    }

    private JsonNode call(String tool, String argsJson) throws Exception {
        return rpc("tools/call", "{\"name\":\"" + tool + "\",\"arguments\":" + argsJson + "}").path("result");
    }

    @Test
    void initializeAdvertisesCapabilities() throws Exception {
        JsonNode r = rpc("initialize", "{\"protocolVersion\":\"2025-06-18\",\"capabilities\":{},"
                + "\"clientInfo\":{\"name\":\"junit\",\"version\":\"1\"}}").path("result");
        assertEquals("2025-06-18", r.path("protocolVersion").asText());
        assertTrue(r.path("capabilities").has("tools"));
        assertTrue(r.path("capabilities").has("resources"));
    }

    @Test
    void notificationsProduceNoResponse() {
        assertTrue(server.handle("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}").isEmpty());
    }

    @Test
    void listsAllToolsWithSchemas() throws Exception {
        JsonNode tools = rpc("tools/list", "{}").path("result").path("tools");
        assertEquals(7, tools.size());
        for (JsonNode t : tools) assertEquals("object", t.path("inputSchema").path("type").asText());
    }

    @Test
    void createListAndCompleteTask() throws Exception {
        JsonNode created = call("create_task", "{\"title\":\"Write MCP blog post\",\"priority\":\"high\",\"due_date\":\"2030-01-01\"}");
        assertFalse(created.path("isError").asBoolean());
        long id = mapper.readTree(created.path("content").get(0).path("text").asText()).path("id").asLong();

        JsonNode listed = mapper.readTree(call("list_tasks", "{\"status\":\"open\"}").path("content").get(0).path("text").asText());
        assertEquals(1, listed.size());

        JsonNode done = call("update_task_status", "{\"id\":" + id + ",\"status\":\"done\"}");
        JsonNode task = mapper.readTree(done.path("content").get(0).path("text").asText());
        assertEquals("done", task.path("status").asText());
        assertFalse(task.path("completedAt").isNull());
    }

    @Test
    void invalidArgumentsAreToolErrorsNotProtocolErrors() throws Exception {
        JsonNode r = call("create_task", "{\"priority\":\"urgent\"}");
        assertTrue(r.path("isError").asBoolean());
        assertTrue(r.path("content").get(0).path("text").asText().contains("title"));

        JsonNode badEnum = call("create_task", "{\"title\":\"x\",\"priority\":\"urgent\"}");
        assertTrue(badEnum.path("isError").asBoolean());
    }

    @Test
    void readOnlyQueryRejectsWritesAndInjection() throws Exception {
        call("create_task", "{\"title\":\"a\"}");
        assertTrue(call("run_readonly_query", "{\"sql\":\"DELETE FROM tasks\"}").path("isError").asBoolean());
        assertTrue(call("run_readonly_query", "{\"sql\":\"SELECT 1; DROP TABLE tasks\"}").path("isError").asBoolean());
        assertTrue(call("run_readonly_query",
                "{\"sql\":\"WITH x AS (SELECT 1) INSERT INTO tasks(title) SELECT 'p' FROM x\"}").path("isError").asBoolean());

        JsonNode ok = call("run_readonly_query", "{\"sql\":\"SELECT COUNT(*) AS n FROM tasks\"}");
        assertFalse(ok.path("isError").asBoolean());
        assertEquals(1, repo.stats().get("total"));
    }

    @Test
    void deleteRequiresConfirmation() throws Exception {
        call("create_task", "{\"title\":\"keep me\"}");
        assertTrue(call("delete_task", "{\"id\":1,\"confirm\":false}").path("isError").asBoolean());
        assertEquals(1, repo.stats().get("total"));
    }

    @Test
    void readsResources() throws Exception {
        JsonNode r = rpc("resources/read", "{\"uri\":\"tasks://schema\"}").path("result");
        assertTrue(r.path("contents").get(0).path("text").asText().contains("CREATE TABLE"));
    }

    @Test
    void unknownMethodAndMalformedJson() throws Exception {
        assertEquals(-32601, rpc("does/not/exist", "{}").path("error").path("code").asInt());
        JsonNode parse = mapper.readTree(server.handle("{not json").orElseThrow());
        assertEquals(-32700, parse.path("error").path("code").asInt());
    }
}
