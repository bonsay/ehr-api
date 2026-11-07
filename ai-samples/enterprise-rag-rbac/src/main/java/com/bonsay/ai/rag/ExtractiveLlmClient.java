package com.bonsay.ai.rag;

import com.bonsay.ai.llm.HashingEmbedder;
import com.bonsay.ai.llm.LlmClient;
import com.bonsay.ai.llm.LlmRequest;
import com.bonsay.ai.llm.LlmResponse;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Offline stand-in for Claude so the chatbot runs without an API key: picks the context
 * sentences with the highest word overlap with the question and cites their document.
 */
public class ExtractiveLlmClient implements LlmClient {

    private static final Pattern DOC = Pattern.compile("<document id=\"([^\"]+)\"[^>]*>(.*?)</document>", Pattern.DOTALL);
    private static final Pattern QUESTION = Pattern.compile("<question>(.*?)</question>", Pattern.DOTALL);

    @Override
    public LlmResponse complete(LlmRequest request) {
        String prompt = request.lastUserMessage();
        Matcher qm = QUESTION.matcher(prompt);
        Set<String> q = new HashSet<>(HashingEmbedder.tokenize(qm.find() ? qm.group(1) : prompt));

        record Candidate(String doc, String sentence, double score) {}
        List<Candidate> candidates = new ArrayList<>();
        Matcher dm = DOC.matcher(prompt);
        while (dm.find()) {
            String docId = dm.group(1).replaceAll("#\\d+$", "");
            for (String s : dm.group(2).trim().split("(?<=[.!?])\\s+")) {
                long overlap = HashingEmbedder.tokenize(s).stream().filter(q::contains).count();
                if (overlap > 0) candidates.add(new Candidate(docId, s.trim(), overlap));
            }
        }
        if (candidates.isEmpty()) return LlmResponse.of("I don't know based on the provided documents.", "extractive");
        candidates.sort(Comparator.comparingDouble(Candidate::score).reversed());
        StringBuilder sb = new StringBuilder();
        for (Candidate c : candidates.subList(0, Math.min(2, candidates.size()))) {
            sb.append(c.sentence()).append(" [").append(c.doc()).append("] ");
        }
        return LlmResponse.of(sb.toString().trim(), "extractive");
    }
}
