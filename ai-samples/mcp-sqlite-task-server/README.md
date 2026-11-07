# MCP SQLite Task Server

A from-scratch [Model Context Protocol](https://modelcontextprotocol.io) server in Java. It exposes a personal task manager, stored in SQLite, to any MCP client: Claude Desktop, Claude Code, or your own agent.

## What it shows
* MCP over the **stdio transport**: newline-delimited JSON-RPC 2.0 on stdout, logs on stderr
* `initialize` / capability negotiation, `tools/list`, `tools/call`, `resources/list`, `resources/read`, `ping`, notifications
* Tool errors returned as `isError: true` results so the model can self-correct. Protocol errors use JSON-RPC error codes.
* **Argument validation** against each tool's JSON Schema (`required`, `enum`, `type`, bounds, `additionalProperties:false`)
* **Safe ad-hoc SQL**: `run_readonly_query` passes a single-SELECT validator, then runs on a connection opened read-only by the driver *and* with `PRAGMA query_only`. Rows are capped and the query times out.
* Destructive tools need an explicit `confirm: true`, and read-only tools carry `readOnlyHint`

| Tool | Purpose |
|---|---|
| `create_task` | title, description, priority, due date |
| `list_tasks` | filter by status / priority, ordered by priority then due date |
| `search_tasks` | LIKE search with escaped wildcards |
| `update_task_status` | open → in_progress → done (stamps `completed_at`) |
| `delete_task` | requires `confirm: true` |
| `task_stats` | open / in progress / done / overdue counts |
| `run_readonly_query` | one SELECT, at most 200 rows |

Resources: `tasks://schema` (DDL) and `tasks://open` (open tasks as JSON).

## Run
```bash
mvn -pl mcp-sqlite-task-server -am package
java -jar mcp-sqlite-task-server/target/mcp-sqlite-task-server-1.0.0-all.jar --db ~/tasks.db
```

### Claude Desktop (`claude_desktop_config.json`)
```json
{
  "mcpServers": {
    "tasks": {
      "command": "java",
      "args": ["-jar", "/abs/path/mcp-sqlite-task-server-1.0.0-all.jar", "--db", "/abs/path/tasks.db"]
    }
  }
}
```

### Claude Code
```bash
claude mcp add tasks -- java -jar /abs/path/mcp-sqlite-task-server-1.0.0-all.jar --db /abs/path/tasks.db
```

Then ask: *"Add a high-priority task to renew my passport by March 1st, then show me what's overdue."*

## Extending
Implement `McpTool` and register it in `TaskTools.registerAll`. `McpServer` has no transport code (it maps a message string to an optional response string), so you can add an HTTP transport alongside stdio without changing it.
