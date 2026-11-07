package com.bonsay.ai.rag.api;

import com.bonsay.ai.llm.AnthropicClient;
import com.bonsay.ai.llm.HashingEmbedder;
import com.bonsay.ai.llm.Json;
import com.bonsay.ai.llm.LlmClient;
import com.bonsay.ai.rag.ExtractiveLlmClient;
import com.bonsay.ai.rag.RagAnswer;
import com.bonsay.ai.rag.RagService;
import com.bonsay.ai.rag.eval.JsonlTracer;
import com.bonsay.ai.rag.eval.LangSmithTracer;
import com.bonsay.ai.rag.ingest.Chunker;
import com.bonsay.ai.rag.ingest.CorpusLoader;
import com.bonsay.ai.rag.retrieval.VectorStore;
import com.bonsay.ai.rag.security.UserDirectory;
import com.bonsay.ai.rag.security.UserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

/**
 * Minimal HTTP API.
 * <pre>
 * curl -s localhost:8081/ask -H 'X-User-Id: alice' -d '{"question":"What is the Q3 revenue?"}'
 * </pre>
 * The {@code X-User-Id} header stands in for a verified identity (e.g. a JWT {@code sub}).
 */
public final class ChatServer {

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8081"));
        HashingEmbedder embedder = new HashingEmbedder();
        VectorStore store = new VectorStore(embedder, new Chunker(80, 15));
        CorpusLoader.loadDefault().forEach(store::ingest);

        LlmClient llm = AnthropicClient.isConfigured() ? AnthropicClient.fromEnv() : new ExtractiveLlmClient();
        RagService rag = new RagService(store, llm,
                LangSmithTracer.fromEnvOr(new JsonlTracer(Path.of("traces", "rag-traces.jsonl"))));
        UserDirectory users = new UserDirectory();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/ask", ex -> handleAsk(ex, rag, users));
        server.createContext("/health", ex -> send(ex, 200, Map.of("status", "ok", "chunks", store.size())));
        server.start();
        System.out.printf("RAG chatbot on http://localhost:%d (llm=%s)%n", port, llm.getClass().getSimpleName());
    }

    private static void handleAsk(HttpExchange ex, RagService rag, UserDirectory users) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { send(ex, 405, Map.of("error", "POST only")); return; }
        Optional<UserPrincipal> user = users.find(ex.getRequestHeaders().getFirst("X-User-Id"));
        if (user.isEmpty()) { send(ex, 401, Map.of("error", "unknown user")); return; }
        JsonNode body;
        try {
            body = Json.read(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            send(ex, 400, Map.of("error", "invalid JSON")); return;
        }
        RagAnswer a = rag.ask(user.get(), body.path("question").asText(""));
        // Contexts are internal - don't return raw chunks to the client.
        send(ex, 200, Map.of("answer", a.answer(), "citations", a.citations(),
                "guardrails", a.guardrailEvents(), "blocked", a.blocked(), "latencyMs", a.latencyMs()));
    }

    private static void send(HttpExchange ex, int status, Object body) throws IOException {
        byte[] bytes = Json.write(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        try (var os = ex.getResponseBody()) { os.write(bytes); }
    }
}
