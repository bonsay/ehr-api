package com.bonsay.ai.gateway.api;

import com.bonsay.ai.gateway.*;
import com.bonsay.ai.gateway.cache.SemanticCache;
import com.bonsay.ai.gateway.limits.BudgetManager;
import com.bonsay.ai.gateway.limits.TokenBucket;
import com.bonsay.ai.gateway.routing.ComplexityRouter;
import com.bonsay.ai.gateway.routing.ModelTier;
import com.bonsay.ai.gateway.routing.PricingTable;
import com.bonsay.ai.llm.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * <pre>
 * curl -s localhost:8082/v1/complete -H 'X-Tenant: acme' -d '{"prompt":"What is a vector database?"}'
 * curl -s localhost:8082/metrics
 * </pre>
 */
public final class GatewayServer {

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8082"));
        LlmClient llm = AnthropicClient.isConfigured() ? AnthropicClient.fromEnv()
                : new ScriptedLlmClient(r -> "[offline " + r.model() + "] answer to: " + r.lastUserMessage());
        Clock clock = Clock.systemUTC();
        LlmGateway gateway = new LlmGateway(llm,
                new SemanticCache(new HashingEmbedder(), 0.90, Duration.ofHours(6), 5_000, clock),
                new ComplexityRouter(), PricingTable.loadDefault(),
                new BudgetManager(Map.of("acme", 50.0), 10.0, 0.8),
                () -> new TokenBucket(20, 5, clock), clock);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/v1/complete", ex -> {
            try {
                if (!"POST".equals(ex.getRequestMethod())) throw new GatewayException(405, "POST only");
                JsonNode b = Json.read(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                String tier = b.path("tier").asText(null);
                GatewayResponse r = gateway.complete(new GatewayRequest(ex.getRequestHeaders().getFirst("X-Tenant"),
                        b.path("system").asText(""), b.path("prompt").asText(""), b.path("temperature").asDouble(0),
                        tier == null ? null : ModelTier.valueOf(tier), b.path("cache").asBoolean(true)));
                send(ex, 200, r);
            } catch (GatewayException e) {
                send(ex, e.status(), Map.of("error", e.getMessage()));
            } catch (IllegalArgumentException e) {
                send(ex, 400, Map.of("error", e.getMessage()));
            } catch (LlmException e) {
                send(ex, 502, Map.of("error", "upstream model error"));
            }
        });
        server.createContext("/metrics", ex -> send(ex, 200, gateway.metrics().snapshot()));
        server.start();
        System.out.printf("LLM gateway on http://localhost:%d (llm=%s)%n", port, llm.getClass().getSimpleName());
    }

    private static void send(HttpExchange ex, int status, Object body) throws IOException {
        byte[] bytes = Json.write(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        try (var os = ex.getResponseBody()) { os.write(bytes); }
    }
}
