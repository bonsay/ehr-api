package com.bonsay.ai.rag.guardrails;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Screens user input before retrieval: length limits, prompt-injection / jailbreak heuristics,
 * attempts to escalate privileges, and PII (which is redacted, not blocked). A production
 * system would add a classifier model here; the heuristics are a fast, cheap first layer.
 */
public class InputGuardrail {

    private static final int MAX_CHARS = 2000;
    private static final List<Pattern> INJECTION = List.of(
            Pattern.compile("ignore (all |any )?(previous|prior|above) (instructions|rules)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(reveal|print|show|repeat) (your|the) (system )?(prompt|instructions)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("you are now (in )?(dan|developer mode|unrestricted)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("disregard (your|the) (guidelines|policy|policies)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("</?(system|document|context)>", Pattern.CASE_INSENSITIVE));
    private static final List<Pattern> ESCALATION = List.of(
            Pattern.compile("(i am|i'm|act as|pretend (to be|you are)) (an? |the )?(admin|administrator|executive|cfo|ceo|hr)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(grant|give) me (access|permission)", Pattern.CASE_INSENSITIVE));

    public GuardrailResult check(String input) {
        List<String> violations = new ArrayList<>();
        if (input == null || input.isBlank()) return new GuardrailResult(false, "", List.of("EMPTY_INPUT"));
        if (input.length() > MAX_CHARS) violations.add("INPUT_TOO_LONG");
        for (Pattern p : INJECTION) if (p.matcher(input).find()) { violations.add("PROMPT_INJECTION"); break; }
        for (Pattern p : ESCALATION) if (p.matcher(input).find()) { violations.add("PRIVILEGE_ESCALATION"); break; }
        if (!violations.isEmpty()) return new GuardrailResult(false, input, violations);

        PiiRedactor.Redaction r = PiiRedactor.redact(input);
        List<String> notes = r.types().stream().map(t -> "PII_REDACTED:" + t).toList();
        return new GuardrailResult(true, r.text(), notes);
    }
}
