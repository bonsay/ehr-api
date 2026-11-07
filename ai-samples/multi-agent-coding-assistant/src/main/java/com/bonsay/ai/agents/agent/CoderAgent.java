package com.bonsay.ai.agents.agent;

import com.bonsay.ai.agents.CodingState;
import com.bonsay.ai.llm.LlmClient;
import com.bonsay.ai.llm.LlmRequest;

import java.util.function.UnaryOperator;

/** Writes (or repairs) the implementation from the spec plus any failure feedback. */
public class CoderAgent implements UnaryOperator<CodingState> {

    public static final String SYSTEM = """
            ROLE: CODER. You are an expert Java engineer.
            Write the complete Java 21 class described by the spec. No package declaration.
            Use only java.lang and java.util. No I/O, networking, reflection, threads or System.exit.
            Return a single ```java code block and nothing else.""";

    private final LlmClient llm;

    public CoderAgent(LlmClient llm) { this.llm = llm; }

    @Override
    public CodingState apply(CodingState s) {
        StringBuilder prompt = new StringBuilder()
                .append("Task: ").append(s.task).append('\n')
                .append("Class: ").append(s.spec.className()).append('\n')
                .append("Signature: ").append(s.spec.methodSignature()).append('\n')
                .append("Steps:\n");
        s.spec.steps().forEach(st -> prompt.append("- ").append(st).append('\n'));
        prompt.append("Edge cases:\n");
        s.spec.edgeCases().forEach(e -> prompt.append("- ").append(e).append('\n'));
        if (s.sourceCode != null && !s.feedback.isEmpty()) {
            prompt.append("\nYour previous attempt:\n```java\n").append(s.sourceCode).append("\n```\n")
                  .append("It FAILED with:\n").append(s.feedback.get(s.feedback.size() - 1))
                  .append("\nFix the implementation. Do not change the signature.");
        }
        s.sourceCode = LlmOutput.extractJava(llm.complete(LlmRequest.of(SYSTEM, prompt.toString())).text());
        s.coderAttempts++;
        s.log.add("coder: attempt " + s.coderAttempts + " (" + s.sourceCode.lines().count() + " lines)");
        return s;
    }
}
