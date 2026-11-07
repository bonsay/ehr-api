package com.bonsay.ai.rag.eval;

import com.bonsay.ai.llm.Embedder;
import com.bonsay.ai.llm.Json;
import com.bonsay.ai.rag.RagAnswer;
import com.bonsay.ai.rag.RagService;
import com.bonsay.ai.rag.guardrails.OutputGuardrail;
import com.bonsay.ai.rag.security.UserDirectory;
import com.bonsay.ai.rag.security.UserPrincipal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Offline evaluation using Ragas-style metrics, computed locally so they can gate CI:
 * <ul>
 *   <li><b>context_precision</b> - share of retrieved citations that are ground-truth relevant</li>
 *   <li><b>context_recall</b> - share of ground-truth docs that were cited</li>
 *   <li><b>faithfulness</b> - share of answer sentences grounded in retrieved context</li>
 *   <li><b>answer_relevancy</b> - embedding similarity between question and answer</li>
 *   <li><b>rbac_leak_rate</b> - cited docs the user is not entitled to (must be 0)</li>
 *   <li><b>refusal_accuracy</b> - refused exactly when expected</li>
 * </ul>
 * It also exports a {@code ragas_dataset.jsonl} (question/answer/contexts/ground_truth) so the
 * same run can be scored by the Python {@code ragas} library with LLM-judged metrics.
 */
public class RagEvaluator {

    private final RagService rag;
    private final UserDirectory users;
    private final Embedder embedder;
    private final Map<String, Set<String>> docAcl;

    public RagEvaluator(RagService rag, UserDirectory users, Embedder embedder, Map<String, Set<String>> docAclByRole) {
        this.rag = rag;
        this.users = users;
        this.embedder = embedder;
        this.docAcl = docAclByRole;
    }

    public record CaseResult(EvalCase evalCase, RagAnswer answer, double contextPrecision, double contextRecall,
                             double faithfulness, double answerRelevancy, int leakedDocs, boolean refusalCorrect) {}

    public record Report(List<CaseResult> cases, Map<String, Double> metrics) {}

    public Report run(List<EvalCase> dataset) {
        List<CaseResult> results = new ArrayList<>();
        for (EvalCase c : dataset) {
            UserPrincipal user = users.find(c.userId()).orElseThrow();
            RagAnswer a = rag.ask(user, c.question());
            boolean refused = a.blocked() || a.citations().isEmpty() || OutputGuardrail.isAbstention(a.answer());
            Set<String> cited = new HashSet<>(a.citations());

            double precision = cited.isEmpty() ? (c.relevantDocIds().isEmpty() ? 1 : 0)
                    : (double) cited.stream().filter(c.relevantDocIds()::contains).count() / cited.size();
            double recall = c.relevantDocIds().isEmpty() ? 1
                    : (double) c.relevantDocIds().stream().filter(cited::contains).count() / c.relevantDocIds().size();
            double faithfulness = refused ? 1 : faithfulness(a);
            double relevancy = refused ? 0 : Embedder.cosine(embedder.embed(c.question()), embedder.embed(a.answer()));
            int leaked = (int) cited.stream().filter(d -> !entitled(user, d)).count();

            results.add(new CaseResult(c, a, precision, recall, faithfulness, relevancy, leaked,
                    refused == c.expectRefusal()));
        }
        return new Report(results, aggregate(results));
    }

    private double faithfulness(RagAnswer a) {
        List<String> sentences = OutputGuardrail.sentences(a.answer());
        if (sentences.isEmpty()) return 0;
        Set<String> ctx = new HashSet<>();
        a.contexts().forEach(t -> ctx.addAll(com.bonsay.ai.llm.HashingEmbedder.tokenize(t)));
        long ok = sentences.stream().filter(s -> {
            List<String> tok = com.bonsay.ai.llm.HashingEmbedder.tokenize(s.replaceAll("\\[[^]]+]", ""));
            return tok.isEmpty() || tok.stream().filter(ctx::contains).count() >= 0.5 * tok.size();
        }).count();
        return (double) ok / sentences.size();
    }

    private boolean entitled(UserPrincipal user, String docId) {
        Set<String> roles = docAcl.getOrDefault(docId, Set.of());
        return user.roles().stream().anyMatch(r -> roles.contains(r.name()));
    }

    private static Map<String, Double> aggregate(List<CaseResult> rs) {
        List<CaseResult> answerable = rs.stream().filter(r -> !r.evalCase().expectRefusal()).toList();
        Map<String, Double> m = new LinkedHashMap<>();
        m.put("context_precision", avg(answerable, CaseResult::contextPrecision));
        m.put("context_recall", avg(answerable, CaseResult::contextRecall));
        m.put("faithfulness", avg(answerable, CaseResult::faithfulness));
        m.put("answer_relevancy", avg(answerable, CaseResult::answerRelevancy));
        m.put("rbac_leak_rate", rs.isEmpty() ? 0 : (double) rs.stream().mapToInt(CaseResult::leakedDocs).sum() / rs.size());
        m.put("refusal_accuracy", avg(rs, r -> r.refusalCorrect() ? 1 : 0));
        return m;
    }

    private static double avg(List<CaseResult> rs, java.util.function.ToDoubleFunction<CaseResult> f) {
        return rs.stream().mapToDouble(f).average().orElse(0);
    }

    /** Writes report.json and a ragas-compatible dataset for further LLM-judged scoring. */
    public static void export(Report report, Path dir) throws IOException {
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("report.json"), Json.write(report.metrics()));
        StringBuilder sb = new StringBuilder();
        for (CaseResult r : report.cases()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("question", r.evalCase().question());
            row.put("answer", r.answer().answer());
            row.put("contexts", r.answer().contexts());
            row.put("ground_truth", r.evalCase().groundTruth());
            sb.append(Json.write(row)).append('\n');
        }
        Files.writeString(dir.resolve("ragas_dataset.jsonl"), sb.toString());
    }
}
