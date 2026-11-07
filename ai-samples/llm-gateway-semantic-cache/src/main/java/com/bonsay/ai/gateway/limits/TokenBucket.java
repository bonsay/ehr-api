package com.bonsay.ai.gateway.limits;

import java.time.Clock;

/** Classic token bucket: {@code capacity} burst, refilled at {@code refillPerSecond}. */
public class TokenBucket {
    private final double capacity;
    private final double refillPerSecond;
    private final Clock clock;
    private double tokens;
    private long lastRefillMillis;

    public TokenBucket(double capacity, double refillPerSecond, Clock clock) {
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
        this.clock = clock;
        this.tokens = capacity;
        this.lastRefillMillis = clock.millis();
    }

    public synchronized boolean tryAcquire() {
        long now = clock.millis();
        tokens = Math.min(capacity, tokens + (now - lastRefillMillis) / 1000.0 * refillPerSecond);
        lastRefillMillis = now;
        if (tokens < 1) return false;
        tokens -= 1;
        return true;
    }
}
