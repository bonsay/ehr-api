package com.bonsay.ai.guardrail.eval;

import com.bonsay.ai.guardrail.*;
import com.bonsay.ai.guardrail.model.SimulatedSupportModel;
import com.bonsay.ai.guardrail.resilience.RetryPolicy;
import com.bonsay.ai.llm.Json;
import com.bonsay.ai.llm.LlmClient;
import com.bonsay.ai.llm.LlmException;
import com.bonsay.ai.llm.LlmResponse;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

/** Runs the golden dataset, scores each case, and writes JSON + Markdown reports for CI. */
public class EvalRunner {

    public record CaseResult(EvalCase evalCase, AgentResult result, boolean passed, List<String> failures) {}

    public record Report(List<CaseResult> results, Map<String, double[]> byCategory, double overallPassRate) {
        public double passRate(String category) {
            double[] c = byCategory.get(category);
            return c == null ? 1.0 : c[0] / c[1];
        }
    }

    private static final Map<String, String> EMAILS = Map.of("C-1", "jane@example.com", "C-2", "omar@example.com");
    private final Supplier<LlmClient> modelFactory;

    public EvalRunner(Supplier<LlmClient> modelFactory) {
        this.modelFactory = modelFactory;
    }

    public static EvalRunner offline() {
        return new EvalRunner(SimulatedSupportModel::new);
    }

    public static List<EvalCase> load(Path jsonl) throws IOException {
        List<EvalCase> cases = new ArrayList<>();
        for (String line : Files.readAllLines(jsonl)) {
            if (line.isBlank() || line.startsWith("//")) continue;
            JsonNode n = Json.read(line);
            cases.add(new EvalCase(n.path("id").asText(), n.path("category").asText(), n.path("customerId").asText("C-1"),
                    n.path("input").asText(), n.path("expectedOutcome").asText(), strings(n.path("expectedTools")),
                    strings(n.path("mustContain")), strings(n.path("mustNotContain")), n.path("chaos").asText("none")));
        }
        return cases;
    }

    public Report run(List<EvalCase> cases) {
        List<CaseResult> results = new ArrayList<>();
        Map<String, double[]> byCat = new TreeMap<>();
        for (EvalCase c : cases) {
            GuardedSupportAgent agent = AgentFactory.create(chaos(c.chaos(), true), null, chaos(c.chaos(), false), null,
                    new RetryPolicy(2, Duration.ofMillis(1), Duration.ofMillis(5), RetryPolicy.Sleeper.NONE), Duration.ofMillis(300), e -> {});
            AgentResult r = agent.handle(new Session(c.customerId(), EMAILS.getOrDefault(c.customerId(), "x@example.com")), c.input());
            List<String> failures = score(c, r);
            results.add(new CaseResult(c, r, failures.isEmpty(), failures));
            double[] agg = byCat.computeIfAbsent(c.category(), k -> new double[2]);
            agg[0] += failures.isEmpty() ? 1 : 0;
            agg[1] += 1;
        }
        double overall = results.stream().filter(CaseResult::passed).count() / (double) Math.max(1, results.size());
        return new Report(results, byCat, overall);
    }

    private LlmClient chaos(String mode, boolean primary) {
        LlmClient real = modelFactory.get();
        return switch (mode) {
            case "primary_down" -> primary ? down() : real;
            case "all_down" -> down();
            case "malformed" -> req -> LlmResponse.of("Sure! Here's what I think: the order is fine :)", "chaos");
            case "slow" -> primary ? req -> { sleep(3_000); return real.complete(req); } : real;
            default -> real;
        };
    }

    private static LlmClient down() {
        return req -> { throw new LlmException("503 overloaded (chaos)", true, 503, null); };
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    static List<String> score(EvalCase c, AgentResult r) {
        List<String> f = new ArrayList<>();
        if (!r.outcome().name().equals(c.expectedOutcome())) f.add("outcome " + r.outcome() + " != " + c.expectedOutcome());
        if (!c.expectedTools().isEmpty() && !r.toolsCalled().equals(c.expectedTools()))
            f.add("tools " + r.toolsCalled() + " != " + c.expectedTools());
        String lower = r.answer().toLowerCase(Locale.ROOT);
        for (String s : c.mustContain()) if (!lower.contains(s.toLowerCase(Locale.ROOT))) f.add("missing '" + s + "'");
        for (String s : c.mustNotContain()) if (lower.contains(s.toLowerCase(Locale.ROOT))) f.add("contains forbidden '" + s + "'");
        return f;
    }

    public static void writeReports(Report report, Path dir) throws IOException {
        Files.createDirectories(dir);
        List<Map<String, Object>> rows = new ArrayList<>();
        StringBuilder md = new StringBuilder("## Guardrail agent eval\n\n| Category | Passed | Rate |\n|---|---|---|\n");
        report.byCategory().forEach((cat, v) -> md.append(String.format(Locale.ROOT, "| %s | %d/%d | %.0f%% |%n",
                cat, (int) v[0], (int) v[1], 100 * v[0] / v[1])));
        md.append(String.format(Locale.ROOT, "%n**Overall: %.1f%%**%n", 100 * report.overallPassRate()));
        for (CaseResult r : report.results()) {
            rows.add(Map.of("id", r.evalCase().id(), "category", r.evalCase().category(), "passed", r.passed(),
                    "outcome", r.result().outcome().name(), "answer", r.result().answer(), "failures", r.failures()));
            if (!r.passed()) md.append("- FAIL `").append(r.evalCase().id()).append("`: ").append(r.failures()).append('\n');
        }
        Files.writeString(dir.resolve("eval-report.json"), Json.write(Map.of("overall", report.overallPassRate(), "cases", rows)));
        Files.writeString(dir.resolve("eval-report.md"), md.toString());
    }

    private static List<String> strings(JsonNode arr) {
        List<String> out = new ArrayList<>();
        arr.forEach(x -> out.add(x.asText()));
        return out;
    }
}
