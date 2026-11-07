package com.bonsay.ai.rag.guardrails;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Detects and masks common PII: US SSNs, payment cards (Luhn-checked), emails, phone numbers. */
public final class PiiRedactor {

    private static final Pattern SSN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern CARD = Pattern.compile("\\b(?:\\d[ -]?){13,19}\\b");
    private static final Pattern EMAIL = Pattern.compile("\\b[\\w.+-]+@[\\w-]+\\.[\\w.-]+\\b");
    private static final Pattern PHONE = Pattern.compile("\\b\\(?\\d{3}\\)?[ .-]\\d{3}[ .-]\\d{4}\\b");

    private PiiRedactor() {}

    public record Redaction(String text, List<String> types) {}

    public static Redaction redact(String input) {
        List<String> found = new ArrayList<>();
        String out = replace(SSN, input, "[REDACTED-SSN]", "SSN", found);
        out = replaceCards(out, found);
        out = replace(EMAIL, out, "[REDACTED-EMAIL]", "EMAIL", found);
        out = replace(PHONE, out, "[REDACTED-PHONE]", "PHONE", found);
        return new Redaction(out, found);
    }

    private static String replace(Pattern p, String in, String mask, String type, List<String> found) {
        Matcher m = p.matcher(in);
        if (!m.find()) return in;
        found.add(type);
        return m.replaceAll(mask);
    }

    private static String replaceCards(String in, List<String> found) {
        Matcher m = CARD.matcher(in);
        StringBuilder sb = new StringBuilder();
        boolean any = false;
        while (m.find()) {
            String digits = m.group().replaceAll("[ -]", "");
            if (luhn(digits)) {
                m.appendReplacement(sb, "[REDACTED-CARD]");
                any = true;
            } else {
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group()));
            }
        }
        m.appendTail(sb);
        if (any) found.add("CARD");
        return sb.toString();
    }

    static boolean luhn(String digits) {
        int sum = 0;
        boolean dbl = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (dbl) { d *= 2; if (d > 9) d -= 9; }
            sum += d;
            dbl = !dbl;
        }
        return digits.length() >= 13 && sum % 10 == 0;
    }
}
