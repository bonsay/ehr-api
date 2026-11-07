package com.bonsay.ai.gateway;

import com.bonsay.ai.gateway.cache.SemanticCache;
import com.bonsay.ai.gateway.limits.BudgetManager;
import com.bonsay.ai.gateway.limits.TokenBucket;
import com.bonsay.ai.gateway.routing.ComplexityRouter;
import com.bonsay.ai.gateway.routing.ModelTier;
import com.bonsay.ai.gateway.routing.PricingTable;
import com.bonsay.ai.llm.*;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Request pipeline:
 * <pre>
 * rate limit -> budget check -> cache lookup -> route (auto / override, budget downgrade) -> LLM
 *            -> cost accounting -> cache store -> metrics
 * </pre>
 */
public class LlmGateway {

    private final LlmClient llm;
    private final SemanticCache cache;
    private final ComplexityRouter router;
    private final PricingTable pricing;
    private final BudgetManager budgets;
    private final Supplier<TokenBucket> bucketFactory;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final Metrics metrics = new Metrics();
    private final Clock clock;

    public LlmGateway(LlmClient llm, SemanticCache cache, ComplexityRouter router, PricingTable pricing,
                      BudgetManager budgets, Supplier<TokenBucket> bucketFactory, Clock clock) {
        this.llm = llm;
        this.cache = cache;
        this.router = router;
        this.pricing = pricing;
        this.budgets = budgets;
        this.bucketFactory = bucketFactory;
        this.clock = clock;
    }

    public GatewayResponse complete(GatewayRequest req) {
        long start = clock.millis();
        metrics.request();
        if (req.tenant() == null || req.tenant().isBlank()) throw reject(400, "tenant required");
        if (!buckets.computeIfAbsent(req.tenant(), t -> bucketFactory.get()).tryAcquire()) throw reject(429, "rate limit exceeded");
        BudgetManager.Status budget = budgets.status(req.tenant());
        if (budget == BudgetManager.Status.EXHAUSTED) throw reject(402, "monthly budget exhausted");

        // Cache scope includes the system prompt so different personas never share answers.
        String scope = Integer.toHexString(req.system().hashCode());
        boolean cacheable = req.cacheable() && req.temperature() == 0.0;
        ComplexityRouter.Decision decision = router.route(req.prompt());
        ModelTier tier = req.tierOverride() != null ? req.tierOverride() : decision.tier();
        List<String> reasons = new ArrayList<>(decision.reasons());
        if (budget == BudgetManager.Status.SOFT_LIMIT && tier != ModelTier.FAST) {
            tier = tier.downgrade();
            reasons.add("budget_soft_limit_downgrade");
        }
        PricingTable.Price price = pricing.price(tier);

        if (cacheable) {
            Optional<SemanticCache.Hit> hit = cache.lookup(req.tenant(), scope, req.prompt());
            if (hit.isPresent()) {
                int estIn = TokenEstimator.estimate(req.system() + req.prompt());
                metrics.hit(price.cost(estIn, TokenEstimator.estimate(hit.get().response())));
                long ms = clock.millis() - start;
                metrics.latency(ms);
                reasons.add(hit.get().exact() ? "cache_exact" : "cache_semantic");
                return new GatewayResponse(hit.get().response(), hit.get().cachedModel(), tier.name(), true,
                        hit.get().similarity(), 0.0, ms, reasons);
            }
        }

        LlmResponse r = llm.complete(new LlmRequest(price.model(), req.system(),
                List.of(ChatMessage.user(req.prompt())), 1024, req.temperature()));
        double cost = price.cost(r.inputTokens(), r.outputTokens());
        budgets.record(req.tenant(), cost);
        metrics.miss(tier.name(), cost);
        if (cacheable) cache.put(req.tenant(), scope, req.prompt(), r.text(), r.model());
        long ms = clock.millis() - start;
        metrics.latency(ms);
        return new GatewayResponse(r.text(), r.model(), tier.name(), false, 0, cost, ms, reasons);
    }

    public Metrics metrics() { return metrics; }

    public BudgetManager budgets() { return budgets; }

    private GatewayException reject(int status, String msg) {
        metrics.rejected();
        return new GatewayException(status, msg);
    }
}
