package com.bonsay.ai.rag.eval;

import com.bonsay.ai.llm.Json;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Posts each run to LangSmith's REST API ({@code POST /runs}) asynchronously so tracing never
 * adds latency or failure modes to the user path. Enabled when {@code LANGSMITH_API_KEY} is set.
 */
public class LangSmithTracer implements Tracer {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final String apiKey;
    private final String endpoint;
    private final String project;

    public LangSmithTracer(String apiKey, String endpoint, String project) {
        this.apiKey = apiKey;
        this.endpoint = endpoint == null ? "https://api.smith.langchain.com" : endpoint;
        this.project = project == null ? "enterprise-rag-rbac" : project;
    }

    public static Tracer fromEnvOr(Tracer fallback) {
        String key = System.getenv("LANGSMITH_API_KEY");
        if (key == null || key.isBlank()) return fallback;
        Tracer ls = new LangSmithTracer(key, System.getenv("LANGSMITH_ENDPOINT"), System.getenv("LANGSMITH_PROJECT"));
        return (n, i, o) -> { fallback.trace(n, i, o); ls.trace(n, i, o); };
    }

    @Override
    public void trace(String name, Map<String, Object> inputs, Object outputs) {
        String now = Instant.now().toString();
        Map<String, Object> run = new LinkedHashMap<>();
        run.put("id", UUID.randomUUID().toString());
        run.put("name", name);
        run.put("run_type", "chain");
        run.put("inputs", inputs);
        run.put("outputs", Map.of("result", outputs));
        run.put("start_time", now);
        run.put("end_time", now);
        run.put("session_name", project);
        HttpRequest req = HttpRequest.newBuilder(URI.create(endpoint + "/runs"))
                .timeout(Duration.ofSeconds(10))
                .header("x-api-key", apiKey)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(Json.write(run)))
                .build();
        http.sendAsync(req, HttpResponse.BodyHandlers.discarding())
            .exceptionally(e -> { System.err.println("[langsmith] " + e.getMessage()); return null; });
    }
}
