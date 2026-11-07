package com.bonsay.ai.rag.guardrails;

import com.bonsay.ai.llm.HashingEmbedder;
import com.bonsay.ai.rag.retrieval.VectorStore.ScoredChunk;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Validates a model answer before it reaches the user:
 * <ol>
 *   <li>Citations must reference only documents that were actually retrieved for this user
 *       (blocks hallucinated or cross-tenant citations).</li>
 *   <li>Grounding: each sentence must have lexical support in the retrieved context; if too many
 *       sentences are unsupported the answer is replaced with a safe refusal.</li>
 *   <li>PII is redacted from the output.</li>
 * </ol>
 */
public class OutputGuardrail {

    public static final String SAFE_FALLBACK =
            "I couldn't find a reliable answer to that in the documents you have access to.";
    private static final Pattern CITATION = Pattern.compile("\\[([A-Za-z0-9_-]+)(?:#\\d+)?]");
    private static final double MIN_SUPPORT = 0.5;
    private static final double MIN_GROUNDED_FRACTION = 0.6;

    public GuardrailResult check(String answer, List<ScoredChunk> context) {
        List<String> violations = new ArrayList<>();
        Set<String> allowedDocIds = context.stream().map(c -> c.chunk().documentId()).collect(Collectors.toSet());

        Matcher m = CITATION.matcher(answer);
        while (m.find()) {
            if (!allowedDocIds.contains(m.group(1))) violations.add("INVALID_CITATION:" + m.group(1));
        }
        if (!violations.isEmpty()) return new GuardrailResult(false, SAFE_FALLBACK, violations);

        if (!isAbstention(answer)) {
            double grounded = groundedFraction(answer, context);
            if (grounded < MIN_GROUNDED_FRACTION) {
                return new GuardrailResult(false, SAFE_FALLBACK,
                        List.of(String.format(Locale.ROOT, "UNGROUNDED_ANSWER:%.2f", grounded)));
            }
        }

        PiiRedactor.Redaction r = PiiRedactor.redact(answer);
        r.types().forEach(t -> violations.add("PII_REDACTED:" + t));
        return new GuardrailResult(true, r.text(), violations);
    }

    public static boolean isAbstention(String answer) {
        String a = answer.toLowerCase(Locale.ROOT);
        return a.contains("i don't know") || a.contains("couldn't find") || a.contains("not in the provided");
    }

    /** Fraction of answer sentences whose content words are >= MIN_SUPPORT covered by context. */
    public static double groundedFraction(String answer, List<ScoredChunk> context) {
        Set<String> contextTokens = new HashSet<>();
        for (ScoredChunk c : context) contextTokens.addAll(HashingEmbedder.tokenize(c.chunk().text()));
        List<String> sentences = sentences(answer);
        if (sentences.isEmpty()) return 0;
        long supported = sentences.stream().filter(s -> support(s, contextTokens) >= MIN_SUPPORT).count();
        return (double) supported / sentences.size();
    }

    static double support(String sentence, Set<String> contextTokens) {
        List<String> tokens = HashingEmbedder.tokenize(CITATION.matcher(sentence).replaceAll(""));
        if (tokens.isEmpty()) return 1;
        long hit = tokens.stream().filter(contextTokens::contains).count();
        return (double) hit / tokens.size();
    }

    public static List<String> sentences(String text) {
        return Arrays.stream(text.split("(?<=[.!?])\\s+")).map(String::trim).filter(s -> s.length() > 3).toList();
    }
}
