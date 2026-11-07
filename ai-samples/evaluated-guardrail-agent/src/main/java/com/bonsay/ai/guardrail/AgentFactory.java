package com.bonsay.ai.guardrail;

import com.bonsay.ai.guardrail.policy.ToolPolicy;
import com.bonsay.ai.guardrail.resilience.CircuitBreaker;
import com.bonsay.ai.guardrail.resilience.ResilientLlmClient;
import com.bonsay.ai.guardrail.resilience.RetryPolicy;
import com.bonsay.ai.guardrail.tools.OrderStore;
import com.bonsay.ai.guardrail.tools.SupportTools;
import com.bonsay.ai.llm.LlmClient;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

/** Assembles the production wiring: primary + fallback provider, breakers, retries, policies. */
public final class AgentFactory {
    private AgentFactory() {}

    public static GuardedSupportAgent create(LlmClient primary, String primaryModel, LlmClient secondary,
                                             String secondaryModel, RetryPolicy retry, Duration callTimeout,
                                             Consumer<String> events) {
        ResilientLlmClient llm = new ResilientLlmClient(List.of(
                new ResilientLlmClient.Provider("primary", primary, primaryModel,
                        new CircuitBreaker(3, Duration.ofSeconds(30), Clock.systemUTC())),
                new ResilientLlmClient.Provider("secondary", secondary, secondaryModel,
                        new CircuitBreaker(3, Duration.ofSeconds(30), Clock.systemUTC()))),
                retry, callTimeout, events);
        OrderStore store = new OrderStore();
        return new GuardedSupportAgent(llm, new SupportTools(store), new ToolPolicy(store, new BigDecimal("100.00")), 6);
    }
}
