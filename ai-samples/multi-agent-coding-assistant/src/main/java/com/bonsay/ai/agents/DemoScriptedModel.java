package com.bonsay.ai.agents;

import com.bonsay.ai.llm.LlmRequest;
import com.bonsay.ai.llm.ScriptedLlmClient;

/**
 * Offline demo model that role-plays the three agents for a Roman-numeral task. The coder's
 * first attempt has a realistic bug (no subtractive notation) so the self-repair loop is visible.
 */
public final class DemoScriptedModel {
    private DemoScriptedModel() {}

    static final String PLAN = """
            {"className":"RomanNumerals","methodSignature":"public static String toRoman(int n)",
             "steps":["Validate 1..3999","Greedy subtract values from a descending table including subtractive pairs","Append symbols"],
             "edgeCases":["n=4 -> IV","n=9 -> IX","n=1994 -> MCMXCIV","n=3999 -> MMMCMXCIX","n=0 throws IllegalArgumentException"]}""";

    static final String BUGGY = """
            ```java
            public class RomanNumerals {
                public static String toRoman(int n) {
                    if (n < 1 || n > 3999) throw new IllegalArgumentException("out of range: " + n);
                    int[] v = {1000, 500, 100, 50, 10, 5, 1};
                    String[] s = {"M", "D", "C", "L", "X", "V", "I"};
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < v.length; i++) while (n >= v[i]) { sb.append(s[i]); n -= v[i]; }
                    return sb.toString();
                }
            }
            ```""";

    static final String FIXED = """
            ```java
            public class RomanNumerals {
                private static final int[] V = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
                private static final String[] S = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
                public static String toRoman(int n) {
                    if (n < 1 || n > 3999) throw new IllegalArgumentException("out of range: " + n);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < V.length; i++) while (n >= V[i]) { sb.append(S[i]); n -= V[i]; }
                    return sb.toString();
                }
            }
            ```""";

    static final String TESTS = """
            ```java
            public class GeneratedTests {
                public static void main(String[] args) {
                    Check.eq("one", "I", RomanNumerals.toRoman(1));
                    Check.eq("three", "III", RomanNumerals.toRoman(3));
                    Check.eq("four", "IV", RomanNumerals.toRoman(4));
                    Check.eq("nine", "IX", RomanNumerals.toRoman(9));
                    Check.eq("fifty-eight", "LVIII", RomanNumerals.toRoman(58));
                    Check.eq("1994", "MCMXCIV", RomanNumerals.toRoman(1994));
                    Check.eq("max", "MMMCMXCIX", RomanNumerals.toRoman(3999));
                    Check.throwsEx("zero", IllegalArgumentException.class, () -> RomanNumerals.toRoman(0));
                    Check.throwsEx("too big", IllegalArgumentException.class, () -> RomanNumerals.toRoman(4000));
                    Check.done();
                }
            }
            ```""";

    public static ScriptedLlmClient create() {
        int[] coderCalls = {0};
        return new ScriptedLlmClient((LlmRequest r) -> {
            String sys = r.system();
            if (sys.startsWith("ROLE: PLANNER")) return PLAN;
            if (sys.startsWith("ROLE: TESTER")) return TESTS;
            if (sys.startsWith("ROLE: CODER")) return coderCalls[0]++ == 0 ? BUGGY : FIXED;
            throw new IllegalStateException("unexpected role");
        });
    }
}
