package com.bonsay.ai.agents.sandbox;

/** Trusted helper compiled alongside generated tests. Tests print PASS/FAIL lines we parse. */
final class TestHarness {
    private TestHarness() {}

    static final String CHECK_SOURCE = """
            public final class Check {
                private static int failed = 0;
                private Check() {}
                public static void eq(String name, Object expected, Object actual) {
                    try {
                        if (java.util.Objects.deepEquals(expected, actual)) System.out.println("PASS " + name);
                        else { failed++; System.out.println("FAIL " + name + ": expected <" + expected + "> but was <" + actual + ">"); }
                    } catch (Throwable t) { failed++; System.out.println("FAIL " + name + ": " + t); }
                }
                public static void throwsEx(String name, Class<? extends Throwable> type, Runnable code) {
                    try { code.run(); failed++; System.out.println("FAIL " + name + ": expected " + type.getSimpleName()); }
                    catch (Throwable t) {
                        if (type.isInstance(t)) System.out.println("PASS " + name);
                        else { failed++; System.out.println("FAIL " + name + ": expected " + type.getSimpleName() + " but got " + t); }
                    }
                }
                public static void done() { if (failed > 0) Runtime.getRuntime().halt(1); }
            }
            """;
}
