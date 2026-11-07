package com.bonsay.ai.guardrail;

import com.bonsay.ai.guardrail.model.SimulatedSupportModel;
import com.bonsay.ai.guardrail.resilience.RetryPolicy;
import com.bonsay.ai.llm.AnthropicClient;
import com.bonsay.ai.llm.LlmClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/** Interactive REPL as customer C-1 (jane@example.com). Uses Claude if ANTHROPIC_API_KEY is set. */
public final class SupportAgentApp {
    public static void main(String[] args) throws Exception {
        boolean live = AnthropicClient.isConfigured();
        LlmClient primary = live ? AnthropicClient.fromEnv() : new SimulatedSupportModel();
        LlmClient secondary = live ? AnthropicClient.fromEnv() : new SimulatedSupportModel();
        GuardedSupportAgent agent = AgentFactory.create(primary, live ? "claude-sonnet-5-5" : null,
                secondary, live ? "claude-haiku-5-5" : null, RetryPolicy.defaults(), java.time.Duration.ofSeconds(20), e -> System.err.println("  [event] " + e));
        Session session = new Session("C-1", "jane@example.com");

        System.out.println("Support agent (" + (live ? "Claude" : "simulated model") + "). Try: 'Where is ORD-1001?',"
                + " 'Refund ORD-1003', 'Refund ORD-1002', 'What card did I use for ORD-1001?'. Ctrl-D to exit.");
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while (System.out.printf("> ") != null && (line = in.readLine()) != null) {
            if (line.isBlank()) continue;
            AgentResult r = agent.handle(session, line);
            System.out.println("[" + r.outcome() + "] " + r.answer());
            r.audit().forEach(a -> System.out.println("  audit: " + a.type() + " " + a.detail()));
        }
    }
}
