package com.bonsay.ai.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Dependency-free client for the Claude Messages API ({@code POST /v1/messages}).
 *
 * <p>Configuration via environment: {@code ANTHROPIC_API_KEY} (required),
 * {@code ANTHROPIC_MODEL} (optional default model), {@code ANTHROPIC_BASE_URL} (optional).</p>
 */
public class AnthropicClient implements LlmClient {

    public static final String DEFAULT_MODEL = "claude-sonnet-5-5";
    private static final String API_VERSION = "2023-06-01";

    private final HttpClient http;
    private final String apiKey;
    private final String baseUrl;
    private final String defaultModel;
    private final Duration timeout;

    public AnthropicClient(String apiKey, String baseUrl, String defaultModel, Duration timeout) {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalArgumentException("ANTHROPIC_API_KEY is not set");
        this.apiKey = apiKey;
        this.baseUrl = baseUrl == null ? "https://api.anthropic.com" : baseUrl.replaceAll("/+$", "");
        this.defaultModel = defaultModel == null ? DEFAULT_MODEL : defaultModel;
        this.timeout = timeout;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    /** Builds a client from environment variables. */
    public static AnthropicClient fromEnv() {
        return new AnthropicClient(System.getenv("ANTHROPIC_API_KEY"), System.getenv("ANTHROPIC_BASE_URL"),
                System.getenv("ANTHROPIC_MODEL"), Duration.ofSeconds(60));
    }

    /** True when an API key is configured, so demos can fall back to offline fakes. */
    public static boolean isConfigured() {
        String key = System.getenv("ANTHROPIC_API_KEY");
        return key != null && !key.isBlank();
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        String model = request.model() == null ? defaultModel : request.model();
        ObjectNode body = Json.MAPPER.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", request.maxTokens());
        body.put("temperature", request.temperature());
        if (!request.system().isBlank()) body.put("system", request.system());
        ArrayNode messages = body.putArray("messages");
        for (ChatMessage m : request.messages()) {
            messages.addObject().put("role", m.role()).put("content", m.content());
        }

        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/messages"))
                .timeout(timeout)
                .header("content-type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", API_VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(Json.write(body)))
                .build();

        HttpResponse<String> response;
        try {
            response = http.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new LlmException("Network error calling Claude: " + e.getMessage(), true, -1, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("Interrupted", false, -1, e);
        }

        int status = response.statusCode();
        if (status != 200) {
            // 429 (rate limit), 5xx and 529 (overloaded) are transient; 4xx are caller errors.
            boolean retryable = status == 429 || status >= 500;
            throw new LlmException("Claude API returned " + status + ": " + response.body(), retryable, status, null);
        }

        JsonNode json = Json.read(response.body());
        StringBuilder text = new StringBuilder();
        for (JsonNode block : json.path("content")) {
            if ("text".equals(block.path("type").asText())) text.append(block.path("text").asText());
        }
        JsonNode usage = json.path("usage");
        return new LlmResponse(text.toString(), json.path("model").asText(model),
                usage.path("input_tokens").asInt(), usage.path("output_tokens").asInt(),
                json.path("stop_reason").asText(""));
    }
}
