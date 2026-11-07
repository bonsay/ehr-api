package com.bonsay.ai.agents.sandbox;

import java.util.List;

/** Outcome of compiling and running generated code in the sandbox. */
public record ExecutionResult(boolean compiled, String compileErrors, int exitCode, String stdout, String stderr,
                              boolean timedOut, long durationMs) {

    public static ExecutionResult compileFailure(String errors) {
        return new ExecutionResult(false, errors, -1, "", "", false, 0);
    }

    public List<String> failures() {
        return stdout.lines().filter(l -> l.startsWith("FAIL ")).toList();
    }

    public long passedCount() {
        return stdout.lines().filter(l -> l.startsWith("PASS ")).count();
    }

    public boolean allTestsPassed() {
        return compiled && !timedOut && exitCode == 0 && failures().isEmpty() && passedCount() > 0;
    }

    /** Compact, model-friendly description of what went wrong. */
    public String summary() {
        if (!compiled) return "COMPILATION ERROR:\n" + compileErrors;
        if (timedOut) return "TIMEOUT: execution exceeded the sandbox time limit (infinite loop?)";
        StringBuilder sb = new StringBuilder();
        sb.append("passed=").append(passedCount()).append(" failed=").append(failures().size())
          .append(" exit=").append(exitCode).append('\n');
        failures().forEach(f -> sb.append(f).append('\n'));
        if (!stderr.isBlank()) sb.append("STDERR:\n").append(stderr, 0, Math.min(stderr.length(), 1500));
        return sb.toString();
    }
}
