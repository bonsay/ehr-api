package com.bonsay.ai.guardrail;

import com.bonsay.ai.guardrail.resilience.CircuitBreaker;
import com.bonsay.ai.guardrail.resilience.ResilientLlmClient;
import com.bonsay.ai.guardrail.resilience.RetryPolicy;
import com.bonsay.ai.llm.*;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ResilienceTest {

    static final class MutableClock extends Clock {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId z) { return this; }
        public Instant instant() { return now; }
    }

    private final RetryPolicy noWait = new RetryPolicy(3, Duration.ofMillis(1), Duration.ofMillis(1), RetryPolicy.Sleeper.NONE);

    @Test
    void breakerOpensThenHalfOpensThenCloses() {
        MutableClock clock = new MutableClock();
        CircuitBreaker b = new CircuitBreaker(2, Duration.ofSeconds(10), clock);
        b.recordFailure();
        assertEquals(CircuitBreaker.State.CLOSED, b.state());
        b.recordFailure();
        assertEquals(CircuitBreaker.State.OPEN, b.state());
        assertFalse(b.allowRequest());
        clock.now = clock.now.plusSeconds(11);
        assertEquals(CircuitBreaker.State.HALF_OPEN, b.state());
        b.recordFailure();                                     // failed probe re-opens
        assertEquals(CircuitBreaker.State.OPEN, b.state());
        clock.now = clock.now.plusSeconds(11);
        assertTrue(b.allowRequest());
        b.recordSuccess();
        assertEquals(CircuitBreaker.State.CLOSED, b.state());
    }

    @Test
    void retriesTransientErrorsThenSucceeds() {
        AtomicInteger calls = new AtomicInteger();
        LlmClient flaky = r -> {
            if (calls.incrementAndGet() < 3) throw new LlmException("529 overloaded", true);
            return LlmResponse.of("ok", "m");
        };
        ResilientLlmClient c = new ResilientLlmClient(List.of(provider("p", flaky, 5)), noWait, Duration.ofSeconds(1), e -> {});
        assertEquals("ok", c.complete(LlmRequest.of("", "hi")).text());
        assertEquals(3, calls.get());
    }

    @Test
    void doesNotRetryNonRetryableErrorsAndFallsBack() {
        AtomicInteger primaryCalls = new AtomicInteger();
        LlmClient badRequest = r -> { primaryCalls.incrementAndGet(); throw new LlmException("400 bad request", false); };
        List<String> events = new ArrayList<>();
        ResilientLlmClient c = new ResilientLlmClient(List.of(provider("primary", badRequest, 5),
                provider("secondary", r -> LlmResponse.of("from secondary", "m"), 5)), noWait, Duration.ofSeconds(1), events::add);
        assertEquals("from secondary", c.complete(LlmRequest.of("", "hi")).text());
        assertEquals(1, primaryCalls.get());
        assertTrue(events.contains("FALLBACK_PROVIDER:secondary"));
    }

    @Test
    void openCircuitSkipsProviderWithoutCallingIt() {
        AtomicInteger calls = new AtomicInteger();
        LlmClient down = r -> { calls.incrementAndGet(); throw new LlmException("503", true); };
        ResilientLlmClient.Provider p = provider("primary", down, 2);
        ResilientLlmClient c = new ResilientLlmClient(List.of(p, provider("s", r -> LlmResponse.of("ok", "m"), 2)),
                noWait, Duration.ofSeconds(1), e -> {});
        c.complete(LlmRequest.of("", "1"));                    // trips the breaker (2 failures)
        int afterTrip = calls.get();
        c.complete(LlmRequest.of("", "2"));
        assertEquals(afterTrip, calls.get(), "open breaker must short-circuit");
        assertEquals(CircuitBreaker.State.OPEN, p.breaker().state());
    }

    @Test
    void slowProviderTimesOut() {
        LlmClient slow = r -> { try { Thread.sleep(5_000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } return LlmResponse.of("late", "m"); };
        ResilientLlmClient c = new ResilientLlmClient(List.of(provider("slow", slow, 5)),
                new RetryPolicy(1, Duration.ZERO, Duration.ZERO, RetryPolicy.Sleeper.NONE), Duration.ofMillis(100), e -> {});
        long start = System.nanoTime();
        assertThrows(LlmException.class, () -> c.complete(LlmRequest.of("", "hi")));
        assertTrue((System.nanoTime() - start) / 1_000_000 < 2_000);
    }

    @Test
    void backoffIsBoundedByMaxDelay() {
        RetryPolicy p = new RetryPolicy(10, Duration.ofMillis(100), Duration.ofMillis(800), RetryPolicy.Sleeper.NONE);
        for (int i = 1; i <= 10; i++) assertTrue(p.delayFor(i).toMillis() <= 800);
    }

    private static ResilientLlmClient.Provider provider(String name, LlmClient c, int threshold) {
        return new ResilientLlmClient.Provider(name, c, null, new CircuitBreaker(threshold, Duration.ofMinutes(1), Clock.systemUTC()));
    }
}
