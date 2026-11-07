package com.bonsay.ai.agents.agent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Helpers for pulling structured content out of free-form model output. */
public final class LlmOutput {
    private static final Pattern CODE = Pattern.compile("```(?:java)?\\s*\\n(.*?)```", Pattern.DOTALL);

    private LlmOutput() {}

    public static String extractJava(String text) {
        Matcher m = CODE.matcher(text);
        return m.find() ? m.group(1).trim() : text.trim();
    }

    public static String extractJsonObject(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) throw new IllegalArgumentException("no JSON object in model output");
        return text.substring(start, end + 1);
    }
}
