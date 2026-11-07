package com.bonsay.ai.guardrail.resilience;

import com.bonsay.ai.llm.LlmClient;
import com.bonsay.ai.llm.LlmException;
import com.bonsay.ai.llm.LlmRequest;
import com.bonsay.ai.llm.LlmResponse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Fallback chain of providers (e.g. primary model -> cheaper model -> other region). Each
 * provider has its own circuit breaker; transient errors are retried with backoff; each call
 * has a hard timeout. Throws only when every provider is exhausted.
 */
public class ResilientLlmClient implements LlmClient {

    public record Provider(String name, LlmClient client, String model, CircuitBreaker breaker) {}

    private final List<Provider> providers;
    private final RetryPolicy retry;
    private final Duration callTimeout;
    private final Consumer<String> events;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public ResilientLlmClient(List<Provider> providers, RetryPolicy retry, Duration callTimeout, Consumer<String> events) {
        if (providers.isEmpty()) throw new IllegalArgumentException("at least one provider");
        this.providers = List.copyOf(providers);
        this.retry = retry;
        this.callTimeout = callTimeout;
        this.events = events;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        List<String> errors = new ArrayList<>();
        for (Provider p : providers) {
            if (!p.breaker().allowRequest()) {
                events.accept("CIRCUIT_OPEN:" + p.name());
                errors.add(p.name() + ": circuit open");
                continue;
            }
            for (int attempt = 1; attempt <= retry.maxAttempts(); attempt++) {
                try {
                    LlmResponse r = callWithTimeout(p, p.model() == null ? request : request.withModel(p.model()));
                    p.breaker().recordSuccess();
                    if (p != providers.get(0)) events.accept("FALLBACK_PROVIDER:" + p.name());
                    return r;
                } catch (LlmException e) {
                    p.breaker().recordFailure();
                    errors.add(p.name() + "#" + attempt + ": " + e.getMessage());
                    events.accept("LLM_ERROR:" + p.name() + ":" + (e.isRetryable() ? "retryable" : "fatal"));
                    if (!e.isRetryable() || !p.breaker().allowRequest()) break;
                    if (attempt < retry.maxAttempts()) sleep(retry.delayFor(attempt));
                }
            }
        }
        throw new LlmException("all providers failed: " + errors, false);
    }

    private LlmResponse callWithTimeout(Provider p, LlmRequest request) {
        Future<LlmResponse> f = executor.submit(() -> p.client().complete(request));
        try {
            return f.get(callTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            f.cancel(true);
            throw new LlmException("timeout after " + callTimeout.toMillis() + "ms", true);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof LlmException le) throw le;
            throw new LlmException("provider error: " + e.getCause(), true, -1, e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("interrupted", false);
        }
    }

    private void sleep(Duration d) {
        try {
            retry.sleeper().sleep(d);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
