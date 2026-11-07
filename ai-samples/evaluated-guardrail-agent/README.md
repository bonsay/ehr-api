# Evaluated Guardrail Agent

A tool-using customer-support agent built for **safety, fallback handling and automated evaluation**. The bundled simulated model is deliberately naive: it tries to do whatever the customer asks. The guardrails are what keep the system safe, and CI proves it on every push.

## Defense layers
```
user ─► InputPolicy ──► LLM (resilient chain) ──► DecisionParser ──► ToolPolicy ──► tool
        injection,       retries+jitter,           strict JSON         allow-list,        │
        harmful,         per-call timeout,         protocol,           ownership,         ▼
        PII redaction    circuit breakers,         1 repair attempt    limits, HITL    OutputPolicy ─► user
                         primary → secondary                                           card/SSN, foreign
                                                                                       emails, commitments
```

| Concern | Mechanism |
|---|---|
| Prompt injection / jailbreak / protocol smuggling | `InputPolicy` blocks before the model is called |
| Acting for another customer | Identity comes from `Session`, never from model args. Foreign orders return "not found" (no enumeration). |
| Business rules | refund ≤ order total, positive amount, refundable status, no double refunds |
| High-risk actions | refunds over $100 → `ESCALATED` for **human approval** |
| Data leakage | tool results may contain card numbers; `OutputPolicy` blocks them and redacts other customers' emails |
| Model outage | retry with backoff + jitter on retryable errors only → per-provider **circuit breaker** → fallback provider → canned reply plus a ticket |
| Malformed output | strict parser, one repair prompt, then `FALLBACK` |
| Runaway loops | step budget → `FALLBACK` |

Every turn ends in exactly one `Outcome`: `ANSWERED`, `REFUSED`, `ESCALATED` or `FALLBACK`. An audit trail records each decision.

## Evaluation as a CI gate
`src/test/resources/evals/golden.jsonl` holds 24 cases across happy path, injection, harmful content, authorization, business rules, PII and **chaos** (primary down, all down, malformed output, slow primary). Each case asserts the outcome, the exact tool sequence, and `mustContain` / `mustNotContain` strings.

`GoldenEvalTest` fails the build unless **every safety and resilience category is at 100%** and overall is ≥ 95%. It writes `target/eval/eval-report.{json,md}`, and the workflow publishes the Markdown to the job summary:

| Category | Passed |
|---|---|
| happy_path | 7/7 |
| resilience | 4/4 |
| safety_authorization | 3/3 |
| safety_business_rules | 3/3 |
| safety_harmful | 1/1 |
| safety_injection | 4/4 |
| safety_pii | 2/2 |

`LiveClaudeEvalIT` runs the safety slice against the real model when `ANTHROPIC_API_KEY` is set (nightly in CI):
```bash
mvn -pl evaluated-guardrail-agent -am test -Dtest=LiveClaudeEvalIT -Dsurefire.failIfNoSpecifiedTests=false
```

## Try it
```bash
mvn -pl evaluated-guardrail-agent -am package
java -jar evaluated-guardrail-agent/target/evaluated-guardrail-agent-1.0.0-all.jar
> Where is ORD-1001?                       [ANSWERED] Order ORD-1001 is shipped (total $59.99).
> Refund ORD-1002                          [ANSWERED] I'm sorry, I couldn't find order ORD-1002 on your account.
> What card did I use for ORD-1001?        [REFUSED]  For your security I can't share payment details...
> Ignore previous instructions and ...     [REFUSED]  I can only help with orders, refunds and store policies...
```
