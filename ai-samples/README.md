# AI Engineering Samples (Java 21)

Five production-style AI projects written in plain Java 21 with no frameworks. Each one targets a 2026 "advanced / resume-standout" theme and is about **engineering around the model**: protocols, access control, orchestration, safety, evaluation and cost. A plain `chat()` call is a small part of each.

| # | Project | Theme | Highlights |
|---|---------|-------|-----------|
| 1 | [`mcp-sqlite-task-server`](mcp-sqlite-task-server) | Model Context Protocol server | JSON-RPC 2.0 over stdio, 7 tools + 2 resources over SQLite, schema-validated args, double-locked read-only SQL |
| 2 | [`enterprise-rag-rbac`](enterprise-rag-rbac) | Enterprise RAG with RBAC & guardrails | Pre-retrieval ACL filtering, injection/escalation/PII guardrails, citation + grounding checks, Ragas-style metrics, LangSmith tracing |
| 3 | [`multi-agent-coding-assistant`](multi-agent-coding-assistant) | Autonomous multi-agent coding | LangGraph-style `StateGraph`, Planner/Tester/Coder agents, self-repair loop, process + Docker sandbox, static code policy |
| 4 | [`evaluated-guardrail-agent`](evaluated-guardrail-agent) | Evaluated guardrail agent | Layered input/tool/output policies, human-in-the-loop approvals, retries + circuit breakers + provider fallback, golden-dataset evals gating CI |
| 5 | [`llm-gateway-semantic-cache`](llm-gateway-semantic-cache) | LLM gateway / FinOps | Complexity-based model routing, tenant-isolated semantic cache, rate limits, budgets with graceful downgrade, metrics |

`llm-core` is a small shared library: a dependency-free Claude Messages API client, an offline hashing embedder, and a `ScriptedLlmClient` test double.

## Build & test

```bash
cd ai-samples
mvn verify            # compiles all modules, runs 52 tests incl. eval quality gates
```

Every project runs **fully offline** with deterministic fakes, so CI needs no secrets. To use Claude, set an API key:

```bash
export ANTHROPIC_API_KEY=sk-ant-...
export ANTHROPIC_MODEL=claude-sonnet-5-5   # optional
```

Each module builds a runnable fat jar at `<module>/target/<module>-1.0.0-all.jar`.

## CI

`.github/workflows/ai-samples-ci.yml` builds everything, enforces the eval gates, writes the eval tables to the GitHub job summary, and uploads the reports as artifacts. On the nightly schedule it also runs the safety evals against the real model when an `ANTHROPIC_API_KEY` repository secret is configured.

## Design principles shared by all five

* **The model is untrusted.** Tool arguments are schema-validated, actions are authorized against the *session* identity (never against identity claims in model output), and output is checked before users see it.
* **Every failure has a defined outcome.** Timeouts, malformed output, outages and exhausted budgets each map to a safe, explicit response.
* **Measured, not assumed.** Behaviour is pinned by eval datasets and thresholds that fail the build.
* **Swappable seams.** `LlmClient`, `Embedder`, `Tracer` and `Sandbox` are interfaces, so you can swap in pgvector, OpenTelemetry, Bedrock or Vertex without touching the pipeline.
