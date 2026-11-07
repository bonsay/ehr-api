package com.bonsay.ai.guardrail.policy;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** First line of defence on raw user input. */
public class InputPolicy {

    public enum Action { ALLOW, BLOCK }

    public record Verdict(Action action, String sanitized, String reason) {}

    private static final int MAX_LEN = 4000;
    private static final List<Pattern> INJECTION = List.of(
            Pattern.compile("ignore (all |any |your )?(previous|prior|above|earlier) (instructions|rules|prompts?)"),
            Pattern.compile("(system prompt|developer message|hidden instructions)"),
            Pattern.compile("you are now|act as (an? )?(unrestricted|jailbroken|dan)\\b|developer mode"),
            Pattern.compile("\\{\\s*\"action\"\\s*:"));          // user trying to smuggle protocol JSON
    private static final List<Pattern> ABUSE = List.of(
            Pattern.compile("\\b(i will|i'll|gonna) (kill|hurt|find) you\\b"),
            Pattern.compile("\\b(bomb|weapon|explosive)s?\\b"));
    private static final Pattern CARD = Pattern.compile("\\b(?:\\d[ -]?){13,19}\\b");
    private static final Pattern SSN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");

    public Verdict check(String input) {
        if (input == null || input.isBlank()) return new Verdict(Action.BLOCK, "", "EMPTY");
        if (input.length() > MAX_LEN) return new Verdict(Action.BLOCK, "", "TOO_LONG");
        String lower = input.toLowerCase(Locale.ROOT);
        for (Pattern p : INJECTION) if (p.matcher(lower).find()) return new Verdict(Action.BLOCK, "", "PROMPT_INJECTION");
        for (Pattern p : ABUSE) if (p.matcher(lower).find()) return new Verdict(Action.BLOCK, "", "HARMFUL_CONTENT");
        // Redact sensitive data so it never reaches the model provider or logs.
        String sanitized = SSN.matcher(CARD.matcher(input).replaceAll("[CARD]")).replaceAll("[SSN]");
        return new Verdict(Action.ALLOW, sanitized, sanitized.equals(input) ? null : "PII_REDACTED");
    }
}
