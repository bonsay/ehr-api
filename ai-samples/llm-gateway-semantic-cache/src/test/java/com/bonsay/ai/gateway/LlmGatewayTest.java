package com.bonsay.ai.gateway;

import com.bonsay.ai.gateway.cache.SemanticCache;
import com.bonsay.ai.gateway.limits.BudgetManager;
import com.bonsay.ai.gateway.limits.TokenBucket;
import com.bonsay.ai.gateway.routing.ComplexityRouter;
import com.bonsay.ai.gateway.routing.ModelTier;
import com.bonsay.ai.gateway.routing.PricingTable;
import com.bonsay.ai.llm.HashingEmbedder;
import com.bonsay.ai.llm.ScriptedLlmClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LlmGatewayTest {

    static final class MutableClock extends Clock {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId z) { return this; }
        public Instant instant() { return now; }
    }

    private MutableClock clock;
    private ScriptedLlmClient llm;
    private SemanticCache cache;
    private BudgetManager budgets;
    private LlmGateway gateway;

    @BeforeEach
    void setUp() {
        clock = new MutableClock();
        llm = new ScriptedLlmClient(r -> "answer(" + r.model() + "): " + r.lastUserMessage());
        cache = new SemanticCache(new HashingEmbedder(), 0.80, Duration.ofHours(1), 100, clock);
        budgets = new BudgetManager(Map.of("tiny", 0.0001), 100.0, 0.8);
        gateway = new LlmGateway(llm, cache, new ComplexityRouter(), PricingTable.loadDefault(), budgets,
                () -> new TokenBucket(5, 1, clock), clock);
    }

    @Test
    void routesByComplexity() {
        ComplexityRouter router = new ComplexityRouter();
        assertEquals(ModelTier.FAST, router.route("What is the capital of France?").tier());
        assertEquals(ModelTier.BALANCED, router.route("Fix this exception: ```java\nint x = null;\n```").tier());
        assertEquals(ModelTier.FRONTIER, router.route(
                "Design a multi-region architecture and analyze the trade-offs, then plan the migration step by step.").tier());
    }

    @Test
    void exactAndSemanticCacheHitsSkipTheModel() {
        GatewayResponse first = gateway.complete(GatewayRequest.of("acme", "How do I reset my password?"));
        assertFalse(first.cacheHit());

        GatewayResponse exact = gateway.complete(GatewayRequest.of("acme", "how do I reset my password"));
        assertTrue(exact.cacheHit());
        assertEquals(1.0, exact.similarity());

        GatewayResponse semantic = gateway.complete(GatewayRequest.of("acme", "How can I reset my password please?"));
        assertTrue(semantic.cacheHit(), "similar paraphrase should hit");
        assertEquals(0.0, semantic.costUsd());
        assertEquals(1, llm.callCount());
        assertTrue((double) gateway.metrics().snapshot().get("saved_usd_estimate") > 0);
    }

    @Test
    void differentNumbersNeverShareACacheEntry() {
        gateway.complete(GatewayRequest.of("acme", "Convert 10 USD to EUR"));
        GatewayResponse r = gateway.complete(GatewayRequest.of("acme", "Convert 100 USD to EUR"));
        assertFalse(r.cacheHit());
        assertEquals(2, llm.callCount());
    }

    @Test
    void cacheIsTenantAndSystemPromptIsolated() {
        gateway.complete(GatewayRequest.of("acme", "Summarise our refund policy"));
        assertFalse(gateway.complete(GatewayRequest.of("globex", "Summarise our refund policy")).cacheHit());
        assertFalse(gateway.complete(new GatewayRequest("acme", "You are a pirate.", "Summarise our refund policy",
                0, null, true)).cacheHit());
    }

    @Test
    void nonDeterministicOrOptOutRequestsAreNotCached() {
        gateway.complete(new GatewayRequest("acme", "", "Write a haiku", 0.9, null, true));
        gateway.complete(new GatewayRequest("acme", "", "Write a haiku", 0.9, null, true));
        gateway.complete(new GatewayRequest("acme", "", "What's my balance?", 0, null, false));
        gateway.complete(new GatewayRequest("acme", "", "What's my balance?", 0, null, false));
        assertEquals(4, llm.callCount());
    }

    @Test
    void cacheEntriesExpire() {
        gateway.complete(GatewayRequest.of("acme", "What is RAG?"));
        clock.now = clock.now.plus(Duration.ofHours(2));
        assertFalse(gateway.complete(GatewayRequest.of("acme", "What is RAG?")).cacheHit());
    }

    @Test
    void rateLimitPerTenant() {
        for (int i = 0; i < 5; i++) gateway.complete(GatewayRequest.of("acme", "q" + i));
        GatewayException e = assertThrows(GatewayException.class, () -> gateway.complete(GatewayRequest.of("acme", "q6")));
        assertEquals(429, e.status());
        assertDoesNotThrow(() -> gateway.complete(GatewayRequest.of("globex", "q")), "other tenants unaffected");
        clock.now = clock.now.plusSeconds(2);
        assertDoesNotThrow(() -> gateway.complete(GatewayRequest.of("acme", "q7")), "bucket refills");
    }

    @Test
    void budgetExhaustionRejectsAndSoftLimitDowngrades() {
        gateway.complete(new GatewayRequest("tiny", "", "Design an architecture and analyze trade-offs step by step", 0, null, false));
        GatewayException e = assertThrows(GatewayException.class, () -> gateway.complete(GatewayRequest.of("tiny", "hi")));
        assertEquals(402, e.status());

        BudgetManager soft = new BudgetManager(Map.of("t", 1.0), 1.0, 0.5);
        soft.record("t", 0.6);
        LlmGateway g = new LlmGateway(llm, cache, new ComplexityRouter(), PricingTable.loadDefault(), soft,
                () -> new TokenBucket(5, 1, clock), clock);
        GatewayResponse r = g.complete(new GatewayRequest("t", "", "x", 0, ModelTier.FRONTIER, false));
        assertEquals("BALANCED", r.tier());
        assertTrue(r.routingReasons().contains("budget_soft_limit_downgrade"));
    }

    @Test
    void lruEvictionBoundsMemory() {
        SemanticCache small = new SemanticCache(new HashingEmbedder(), 0.99, Duration.ofHours(1), 3, clock);
        for (int i = 0; i < 10; i++) small.put("t", "s", "question number " + i, "a", "m");
        assertEquals(3, small.size("t"));
    }
}
