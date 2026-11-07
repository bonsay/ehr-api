package com.bonsay.ai.agents.agent;

import com.bonsay.ai.agents.CodingState;
import com.bonsay.ai.llm.LlmClient;
import com.bonsay.ai.llm.LlmRequest;

import java.util.function.UnaryOperator;

/**
 * Writes tests from the SPEC only - it never sees the implementation, so tests encode the
 * requirements instead of rubber-stamping whatever the coder wrote.
 */
public class TesterAgent implements UnaryOperator<CodingState> {

    public static final String TEST_CLASS = "GeneratedTests";

    public static final String SYSTEM = """
            ROLE: TESTER. You are a meticulous QA engineer.
            Write a Java 21 class named GeneratedTests (no package) with a public static void main(String[] args).
            Use the provided helper: Check.eq(String name, Object expected, Object actual) and
            Check.throwsEx(String name, Class<? extends Throwable> type, Runnable code).
            Cover normal cases and every edge case in the spec. End main with Check.done();
            Return a single ```java code block and nothing else.""";

    private final LlmClient llm;

    public TesterAgent(LlmClient llm) { this.llm = llm; }

    @Override
    public CodingState apply(CodingState s) {
        String prompt = "Class under test: " + s.spec.className() + "\nSignature: " + s.spec.methodSignature()
                + "\nTask: " + s.task + "\nEdge cases:\n- " + String.join("\n- ", s.spec.edgeCases());
        if (s.testCode != null && s.lastRun != null && !s.lastRun.compiled()) {
            prompt += "\n\nYour previous tests did not compile:\n" + s.lastRun.compileErrors();
        }
        s.testCode = LlmOutput.extractJava(llm.complete(LlmRequest.of(SYSTEM, prompt)).text());
        s.testerAttempts++;
        s.log.add("tester: wrote " + s.testCode.lines().filter(l -> l.contains("Check.")).count() + " checks");
        return s;
    }
}
