package com.bonsay.ai.agents.agent;

import java.util.List;

/** The planner's structured contract shared by coder and tester so they can work independently. */
public record TaskSpec(String className, String methodSignature, List<String> steps, List<String> edgeCases) {
}
