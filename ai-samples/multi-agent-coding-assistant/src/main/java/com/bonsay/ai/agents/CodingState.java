package com.bonsay.ai.agents;

import com.bonsay.ai.agents.agent.TaskSpec;
import com.bonsay.ai.agents.sandbox.ExecutionResult;

import java.util.ArrayList;
import java.util.List;

/** Shared blackboard passed between agents by the state graph. */
public class CodingState {
    public final String task;
    public TaskSpec spec;
    public String sourceCode;
    public String testCode;
    public ExecutionResult lastRun;
    public List<String> policyViolations = List.of();
    public int coderAttempts;
    public int testerAttempts;
    public final List<String> feedback = new ArrayList<>();
    public final List<String> log = new ArrayList<>();

    public CodingState(String task) {
        this.task = task;
    }

    public boolean succeeded() {
        return lastRun != null && lastRun.allTestsPassed() && policyViolations.isEmpty();
    }
}
