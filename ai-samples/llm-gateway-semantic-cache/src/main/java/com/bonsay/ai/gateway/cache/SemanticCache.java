package com.bonsay.ai.gateway.cache;

import com.bonsay.ai.llm.Embedder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tenant-isolated semantic cache with TTL and LRU eviction.
 *
 * <p>Two lookup paths: an O(1) exact match on the normalised prompt, then a similarity scan.
 * A semantic hit additionally requires the two prompts to contain the <em>same numbers and
 * quoted literals</em> - otherwise "convert 10 USD to EUR" would be served the answer for
 * "convert 100 USD to EUR". The linear scan is fine for thousands of entries per tenant; use an
 * ANN index (HNSW / pgvector / Redis VSS) beyond that.</p>
 */
public class SemanticCache {

    public record Hit(String response, double similarity, boolean exact, String cachedModel) {}

    private static final Pattern LITERALS = Pattern.compile("\\d+(?:[.,]\\d+)?|\"[^\"]+\"|'[^']+'");

    private final Embedder embedder;
    private final double threshold;
    private final Duration ttl;
    private final int maxEntriesPerTenant;
    private final Clock clock;
    private final Map<String, LinkedHashMap<String, Entry>> tenants = new HashMap<>();

    private record Entry(String normalized, float[] vector, Set<String> literals, String response, String model, Instant createdAt) {}

    public SemanticCache(Embedder embedder, double threshold, Duration ttl, int maxEntriesPerTenant, Clock clock) {
        this.embedder = embedder;
        this.threshold = threshold;
        this.ttl = ttl;
        this.maxEntriesPerTenant = maxEntriesPerTenant;
        this.clock = clock;
    }

    public synchronized Optional<Hit> lookup(String tenant, String scope, String prompt) {
        LinkedHashMap<String, Entry> entries = tenants.get(tenant);
        if (entries == null) return Optional.empty();
        evictExpired(entries);
        String key = key(scope, normalize(prompt));
        Entry exact = entries.get(key);              // access-order map: get() refreshes LRU position
        if (exact != null) return Optional.of(new Hit(exact.response(), 1.0, true, exact.model()));

        float[] q = embedder.embed(prompt);
        Set<String> lits = literals(prompt);
        String bestKey = null;
        double best = -1;
        for (Map.Entry<String, Entry> e : entries.entrySet()) {
            if (!e.getKey().startsWith(scope + "|")) continue;
            if (!e.getValue().literals().equals(lits)) continue;
            double s = Embedder.cosine(q, e.getValue().vector());
            if (s > best) { best = s; bestKey = e.getKey(); }
        }
        if (bestKey == null || best < threshold) return Optional.empty();
        Entry hit = entries.get(bestKey);
        return Optional.of(new Hit(hit.response(), best, false, hit.model()));
    }

    public synchronized void put(String tenant, String scope, String prompt, String response, String model) {
        LinkedHashMap<String, Entry> entries = tenants.computeIfAbsent(tenant, t -> new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Entry> eldest) {
                return size() > maxEntriesPerTenant;
            }
        });
        String norm = normalize(prompt);
        entries.put(key(scope, norm), new Entry(norm, embedder.embed(prompt), literals(prompt), response, model, clock.instant()));
    }

    public synchronized int size(String tenant) {
        LinkedHashMap<String, Entry> e = tenants.get(tenant);
        return e == null ? 0 : e.size();
    }

    private void evictExpired(LinkedHashMap<String, Entry> entries) {
        Instant cutoff = clock.instant().minus(ttl);
        entries.values().removeIf(e -> e.createdAt().isBefore(cutoff));
    }

    private static String key(String scope, String normalized) { return scope + "|" + normalized; }

    static String normalize(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\"' ]", " ").replaceAll("\\s+", " ").strip();
    }

    static Set<String> literals(String s) {
        Set<String> out = new TreeSet<>();
        Matcher m = LITERALS.matcher(s.toLowerCase(Locale.ROOT));
        while (m.find()) out.add(m.group());
        return out;
    }
}
