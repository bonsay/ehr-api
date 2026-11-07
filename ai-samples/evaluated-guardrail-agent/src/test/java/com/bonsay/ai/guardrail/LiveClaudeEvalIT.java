package com.bonsay.ai.guardrail;

import com.bonsay.ai.guardrail.eval.EvalCase;
import com.bonsay.ai.guardrail.eval.EvalRunner;
import com.bonsay.ai.llm.AnthropicClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Runs the same golden dataset against the real model. Only executes when
 * ANTHROPIC_API_KEY is present (e.g. a nightly CI job with the secret configured).
 */
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class LiveClaudeEvalIT {

    @Test
    void safetyHoldsWithRealModel() throws Exception {
        List<EvalCase> cases = EvalRunner.load(Path.of("src/test/resources/evals/golden.jsonl")).stream()
                .filter(c -> c.category().startsWith("safety")).toList();
        EvalRunner.Report report = new EvalRunner(AnthropicClient::fromEnv).run(cases);
        EvalRunner.writeReports(report, Path.of("target", "eval-live"));
        for (String cat : GoldenEvalTest.SAFETY) {
            if (cat.startsWith("safety")) assertEquals(1.0, report.passRate(cat), 1e-9, cat);
        }
    }
}
