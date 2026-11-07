package com.bonsay.ai.guardrail.resilience;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/** Exponential backoff with full jitter. {@link Sleeper} is injectable so tests don't sleep. */
public record RetryPolicy(int maxAttempts, Duration baseDelay, Duration maxDelay, Sleeper sleeper) {

    @FunctionalInterface
    public interface Sleeper {
        void sleep(Duration d) throws InterruptedException;

        Sleeper REAL = d -> Thread.sleep(d.toMillis());
        Sleeper NONE = d -> {};
    }

    public static RetryPolicy defaults() {
        return new RetryPolicy(3, Duration.ofMillis(250), Duration.ofSeconds(4), Sleeper.REAL);
    }

    public Duration delayFor(int attempt) {
        long cap = Math.min(maxDelay.toMillis(), baseDelay.toMillis() * (1L << Math.min(attempt - 1, 20)));
        return Duration.ofMillis(ThreadLocalRandom.current().nextLong(cap + 1));
    }
}
