package com.bonsay.ai.agents;

import com.bonsay.ai.agents.agent.CoderAgent;
import com.bonsay.ai.agents.agent.PlannerAgent;
import com.bonsay.ai.agents.agent.TesterAgent;
import com.bonsay.ai.agents.graph.StateGraph;
import com.bonsay.ai.agents.sandbox.CodePolicy;
import com.bonsay.ai.agents.sandbox.ExecutionResult;
import com.bonsay.ai.agents.sandbox.Sandbox;
import com.bonsay.ai.llm.LlmClient;

import java.util.Map;

/**
 * Wires the agents into a graph:
 * <pre>
 *   plan -> write_tests -> write_code -> run_tests --(pass)--------------------> END
 *                ^             ^             |--(tests don't compile)--> write_tests
 *                |             +-------------|--(impl failed, budget left)
 *                                            +--(budget exhausted)-------------> END
 * </pre>
 */
public class CodingWorkflow {

    private final StateGraph.Compiled<CodingState> graph;
    private final int maxCoderAttempts;

    public CodingWorkflow(LlmClient llm, Sandbox sandbox, int maxCoderAttempts) {
        this.maxCoderAttempts = maxCoderAttempts;
        this.graph = new StateGraph<CodingState>()
                .addNode("plan", new PlannerAgent(llm))
                .addNode("write_tests", new TesterAgent(llm))
                .addNode("write_code", new CoderAgent(llm))
                .addNode("run_tests", s -> runTests(s, sandbox))
                .setEntryPoint("plan")
                .addEdge("plan", "write_tests")
                .addEdge("write_tests", "write_code")
                .addEdge("write_code", "run_tests")
                .addConditionalEdge("run_tests", this::route)
                .compile();
    }

    public StateGraph.Result<CodingState> run(String task, StateGraph.Listener<CodingState> listener) {
        return graph.invoke(new CodingState(task), 4 + maxCoderAttempts * 3, listener);
    }

    private CodingState runTests(CodingState s, Sandbox sandbox) {
        s.policyViolations = CodePolicy.check(s.sourceCode + "\n" + s.testCode);
        if (!s.policyViolations.isEmpty()) {
            s.lastRun = ExecutionResult.compileFailure("blocked by sandbox policy: " + s.policyViolations);
            s.feedback.add("SECURITY POLICY VIOLATION " + s.policyViolations
                    + ". Use only pure computation with java.lang/java.util.");
            s.log.add("sandbox: rejected by policy " + s.policyViolations);
            return s;
        }
        s.lastRun = sandbox.run(Map.of(s.spec.className(), s.sourceCode, TesterAgent.TEST_CLASS, s.testCode),
                TesterAgent.TEST_CLASS);
        s.log.add("sandbox: " + s.lastRun.summary().lines().findFirst().orElse("") + " in " + s.lastRun.durationMs() + "ms");
        if (!s.lastRun.allTestsPassed()) s.feedback.add(s.lastRun.summary());
        return s;
    }

    private String route(CodingState s) {
        if (s.succeeded()) return StateGraph.END;
        ExecutionResult r = s.lastRun;
        boolean testsBroken = r != null && !r.compiled() && s.policyViolations.isEmpty()
                && r.compileErrors().contains(TesterAgent.TEST_CLASS) && !r.compileErrors().contains(s.spec.className() + ".java");
        if (testsBroken && s.testerAttempts < 2) return "write_tests";
        return s.coderAttempts < maxCoderAttempts ? "write_code" : StateGraph.END;
    }
}
