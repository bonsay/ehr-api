package com.bonsay.ai.gateway.routing;

import com.bonsay.ai.llm.TokenEstimator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Cheap, explainable heuristic router: scores a prompt's complexity and picks the cheapest tier
 * likely to handle it. In production, calibrate the thresholds against an offline eval set (or
 * replace with a small classifier) and log {@code reasons} for every decision.
 */
public class ComplexityRouter {

    public record Decision(ModelTier tier, int score, List<String> reasons) {}

    private static final Pattern CODE = Pattern.compile("```|\\b(class|def|function|public static|SELECT .* FROM|stack ?trace|exception)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern REASONING = Pattern.compile(
            "\\b(prove|derive|analy[sz]e|trade-?offs?|architect(ure)?|design a|step[- ]by[- ]step|compare|optimi[sz]e|root cause|strategy|plan)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SIMPLE = Pattern.compile(
            "^(what is|what's|define|translate|summari[sz]e|classify|extract|who is|when did|fix the typo)\\b",
            Pattern.CASE_INSENSITIVE);

    public Decision route(String prompt) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        int tokens = TokenEstimator.estimate(prompt);
        if (tokens > 1500) { score += 3; reasons.add("long_prompt:" + tokens); }
        else if (tokens > 400) { score += 1; reasons.add("medium_prompt:" + tokens); }

        if (CODE.matcher(prompt).find()) { score += 2; reasons.add("code"); }
        long reasoningHits = REASONING.matcher(prompt).results().count();
        if (reasoningHits > 0) { score += (int) Math.min(6, 2 * reasoningHits); reasons.add("reasoning:" + reasoningHits); }
        long questions = prompt.chars().filter(c -> c == '?').count();
        if (questions >= 3) { score += 1; reasons.add("multi_question:" + questions); }
        if (SIMPLE.matcher(prompt.strip().toLowerCase(Locale.ROOT)).find() && score < 3) {
            score = Math.max(0, score - 1);
            reasons.add("simple_intent");
        }

        ModelTier tier = score >= 5 ? ModelTier.FRONTIER : score >= 2 ? ModelTier.BALANCED : ModelTier.FAST;
        return new Decision(tier, score, reasons);
    }
}
