package com.bonsay.ai.mcp;

import com.bonsay.ai.mcp.tools.TaskTools;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Entry point. Speaks MCP over stdio: one JSON-RPC message per line on stdin/stdout.
 * Everything human-readable goes to stderr because stdout is reserved for the protocol.
 *
 * <pre>java -jar mcp-sqlite-task-server-1.0.0-all.jar --db ~/tasks.db</pre>
 */
public final class Main {

    public static void main(String[] args) throws Exception {
        Path db = Path.of(System.getProperty("user.home"), ".mcp-tasks.db");
        for (int i = 0; i < args.length; i++) {
            if ("--db".equals(args[i]) && i + 1 < args.length) db = Path.of(args[++i]);
        }

        TaskRepository repo = new TaskRepository(db);
        McpServer server = new McpServer("sqlite-task-manager", "1.0.0");
        TaskTools.registerAll(server, repo);

        System.err.println("[mcp] task server ready, db=" + db.toAbsolutePath());
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.isBlank()) continue;
                server.handle(line).ifPresent(out::println);
            }
        }
    }
}
