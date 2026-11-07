# LLM Gateway with Semantic Cache & Cost-Aware Routing

A multi-tenant gateway that sits between your apps and the model provider. It cuts cost and latency without letting tenants see each other's data.

```
request ─► rate limit (token bucket / tenant) ─► budget check ─► semantic cache ─► router ─► model
                                                  402 if spent      exact → similar     FAST / BALANCED / FRONTIER
                                                  downgrade @ 80%   (tenant + system-     (complexity score,
                                                                     prompt scoped)        override, downgrade)
                                                          cost accounting ◄─ metrics (hit rate, $ saved, p50/p95)
```

## What it shows
* **Complexity router**: explainable features (length, code, reasoning verbs, multi-question, simple intent) map to a tier. Every decision logs its `routingReasons`.
* **Semantic cache** with an O(1) exact path plus a cosine-similarity path, TTL and per-tenant LRU. Two guards make it safe:
  * isolation: the key includes **tenant + system-prompt hash**
  * a **literal guard**: numbers and quoted strings must match, so "convert 10 USD" never gets the answer to "convert 100 USD"
  * only deterministic (`temperature == 0`), opt-in requests are cached
* **FinOps**: per-tenant budgets with a soft limit (automatic tier downgrade) and a hard limit (HTTP 402). Cost is computed from reported token usage and `pricing.properties`. The prices there are illustrative; update them from the provider's pricing page.
* **Metrics**: requests, rejections, hit rate, cost, estimated $ saved by the cache, calls per tier, p50/p95 latency

## Run
```bash
mvn -pl llm-gateway-semantic-cache -am package
java -jar llm-gateway-semantic-cache/target/llm-gateway-semantic-cache-1.0.0-all.jar   # :8082
curl -s localhost:8082/v1/complete -H 'X-Tenant: acme' -d '{"prompt":"How do I reset my password?"}'
curl -s localhost:8082/v1/complete -H 'X-Tenant: acme' -d '{"prompt":"How can I reset my password please?"}'  # cache hit
curl -s localhost:8082/v1/complete -H 'X-Tenant: acme' -d '{"prompt":"Design a multi-region architecture and analyze trade-offs step by step"}'  # FRONTIER
curl -s localhost:8082/metrics
```
Body fields: `prompt`, `system`, `temperature`, `tier` (`FAST|BALANCED|FRONTIER` override), `cache` (bool).

## Production swaps
Linear similarity scan → an ANN index (Redis VSS / pgvector / HNSW) · in-memory state → Redis for horizontal scaling · heuristic router → a small classifier calibrated on your own eval set · `Metrics` → Micrometer / Prometheus.
