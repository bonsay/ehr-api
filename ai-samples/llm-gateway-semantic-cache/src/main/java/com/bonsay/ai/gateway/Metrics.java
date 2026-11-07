package com.bonsay.ai.gateway;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

/** In-process metrics; expose via /metrics or bridge to Micrometer/Prometheus. */
public class Metrics {
    private final AtomicLong requests = new AtomicLong();
    private final AtomicLong cacheHits = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();
    private final DoubleAdder costUsd = new DoubleAdder();
    private final DoubleAdder savedUsd = new DoubleAdder();
    private final Map<String, AtomicLong> byTier = new ConcurrentHashMap<>();
    private final List<Long> latencies = Collections.synchronizedList(new ArrayList<>());

    void request() { requests.incrementAndGet(); }
    void rejected() { rejected.incrementAndGet(); }
    void hit(double saved) { cacheHits.incrementAndGet(); savedUsd.add(saved); }
    void miss(String tier, double cost) { byTier.computeIfAbsent(tier, t -> new AtomicLong()).incrementAndGet(); costUsd.add(cost); }

    void latency(long ms) {
        synchronized (latencies) {
            latencies.add(ms);
            if (latencies.size() > 10_000) latencies.remove(0);
        }
    }

    public long cacheHits() { return cacheHits.get(); }

    public Map<String, Object> snapshot() {
        Map<String, Object> m = new LinkedHashMap<>();
        long req = requests.get();
        m.put("requests", req);
        m.put("rejected", rejected.get());
        m.put("cache_hits", cacheHits.get());
        m.put("cache_hit_rate", req == 0 ? 0 : (double) cacheHits.get() / req);
        m.put("cost_usd", costUsd.sum());
        m.put("saved_usd_estimate", savedUsd.sum());
        m.put("calls_by_tier", new TreeMap<>(byTier));
        List<Long> sorted;
        synchronized (latencies) { sorted = new ArrayList<>(latencies); }
        Collections.sort(sorted);
        m.put("latency_p50_ms", percentile(sorted, 0.50));
        m.put("latency_p95_ms", percentile(sorted, 0.95));
        return m;
    }

    private static long percentile(List<Long> sorted, double p) {
        if (sorted.isEmpty()) return 0;
        return sorted.get((int) Math.min(sorted.size() - 1, Math.ceil(p * sorted.size()) - 1));
    }
}
