package com.bonsay.ai.guardrail.policy;

import com.bonsay.ai.guardrail.Session;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Last line of defence before text reaches the customer. */
public class OutputPolicy {

    public record Verdict(boolean allowed, String text, String reason) {}

    public static final String PAYMENT_REFUSAL =
            "For your security I can't share payment details in chat. You can view them in your account settings.";
    public static final String GENERIC_REFUSAL = "I'm sorry, I can't help with that. A support specialist can assist you further.";

    private static final Pattern CARD = Pattern.compile("\\b(?:\\d[ -]?){13,19}\\b");
    private static final Pattern SSN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+");
    private static final List<String> FORBIDDEN_COMMITMENTS = List.of(
            "i guarantee", "legal advice", "you should sue", "lawsuit", "medical advice");
    private static final int MAX_LEN = 1200;

    public Verdict check(Session session, String answer) {
        if (CARD.matcher(answer).find() || SSN.matcher(answer).find()) {
            return new Verdict(false, PAYMENT_REFUSAL, "SENSITIVE_DATA_IN_OUTPUT");
        }
        String lower = answer.toLowerCase(Locale.ROOT);
        for (String f : FORBIDDEN_COMMITMENTS) {
            if (lower.contains(f)) return new Verdict(false, GENERIC_REFUSAL, "FORBIDDEN_COMMITMENT");
        }
        // Only the signed-in customer's own email may appear.
        Matcher m = EMAIL.matcher(answer);
        StringBuilder sb = new StringBuilder();
        boolean redacted = false;
        while (m.find()) {
            boolean own = m.group().equalsIgnoreCase(session.customerEmail());
            m.appendReplacement(sb, own ? Matcher.quoteReplacement(m.group()) : "[redacted email]");
            redacted |= !own;
        }
        m.appendTail(sb);
        String text = sb.length() > MAX_LEN ? sb.substring(0, MAX_LEN) + "..." : sb.toString();
        return new Verdict(true, text, redacted ? "FOREIGN_EMAIL_REDACTED" : null);
    }
}
