package com.bonsay.ai.guardrail.resilience;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Classic three-state breaker. After {@code failureThreshold} consecutive failures it OPENs and
 * rejects calls immediately for {@code openDuration}; then allows one HALF_OPEN probe.
 */
public class CircuitBreaker {

    public enum State { CLOSED, OPEN, HALF_OPEN }

    private final int failureThreshold;
    private final Duration openDuration;
    private final Clock clock;
    private State state = State.CLOSED;
    private int consecutiveFailures;
    private Instant openedAt;

    public CircuitBreaker(int failureThreshold, Duration openDuration, Clock clock) {
        this.failureThreshold = failureThreshold;
        this.openDuration = openDuration;
        this.clock = clock;
    }

    public synchronized boolean allowRequest() {
        if (state == State.OPEN && clock.instant().isAfter(openedAt.plus(openDuration))) state = State.HALF_OPEN;
        return state != State.OPEN;
    }

    public synchronized void recordSuccess() {
        consecutiveFailures = 0;
        state = State.CLOSED;
    }

    public synchronized void recordFailure() {
        consecutiveFailures++;
        if (state == State.HALF_OPEN || consecutiveFailures >= failureThreshold) {
            state = State.OPEN;
            openedAt = clock.instant();
        }
    }

    public synchronized State state() {
        allowRequest();
        return state;
    }
}
