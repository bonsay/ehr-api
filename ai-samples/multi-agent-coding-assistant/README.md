# Autonomous Multi-Agent Coding Assistant

Planner, Tester and Coder agents cooperate through a **LangGraph-style state graph** written in Java. Generated code compiles and runs in a **sandbox**, and failures feed back to the Coder until the tests pass or the budget runs out.

```
 plan ──► write_tests ──► write_code ──► run_tests ──(all pass)────────────► END
               ▲               ▲             │
               │               └─(failed, attempts left)
               └───────(tests don't compile)─┘          (budget exhausted) ► END
```

## What it shows
* **`StateGraph<S>`**: nodes, static and conditional edges, a validated compile step, a step budget against runaway loops, and a per-step listener for streaming and tracing
* **Separation of concerns**: the Tester writes tests from the *spec only* and never sees the implementation, so tests encode requirements and don't just rubber-stamp the code
* **Structured hand-offs**: the Planner emits a JSON `TaskSpec` (class, signature, steps, edge cases) that both downstream agents consume
* **Self-repair**: the Coder gets its previous code plus the exact failing assertions (`FAIL four: expected <IV> but was <IIII>`)
* **Sandbox, defense in depth**:
  1. `CodePolicy`, a static deny-list (process exec, network, filesystem, reflection, class loading, `Unsafe`, `System.exit/getenv`, threads), applied before anything runs
  2. `ProcessSandbox`: in-process `javac` with `-proc:none` (compiling never executes code), then a separate JVM with an **empty environment** (no API keys leak), a temp working dir, a 128 MB heap cap, a wall-clock timeout with forced kill, and capped output
  3. `DockerSandbox`: the same plus `--network none --read-only --cap-drop ALL --pids-limit 64 --memory 256m`, running as user `nobody`

## Run
```bash
mvn -pl multi-agent-coding-assistant -am package
# offline demo (scripted agents; the first attempt has a real bug that gets fixed):
java -jar multi-agent-coding-assistant/target/multi-agent-coding-assistant-1.0.0-all.jar
# with Claude, any task:
ANTHROPIC_API_KEY=... java -jar multi-agent-coding-assistant/target/multi-agent-coding-assistant-1.0.0-all.jar \
  "Implement a function that returns the longest palindromic substring"
```
It uses Docker automatically when available. Set `SANDBOX=process` to force the process sandbox.

```
[01] plan        planner: RomanNumerals :: public static String toRoman(int n)
[02] write_tests tester: wrote 10 checks
[03] write_code  coder: attempt 1 (10 lines)
[04] run_tests   sandbox: passed=5 failed=4 exit=1 in 50ms
[05] write_code  coder: attempt 2 (10 lines)
[06] run_tests   sandbox: passed=9 failed=0 exit=0 in 44ms
```

## Ideas to extend
A Reviewer agent node (lint and complexity gate), parallel candidate generation with best-of-n selection, persisted checkpoints so a run can resume after a crash, and multi-file projects built with Maven inside the container.
