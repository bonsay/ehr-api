package com.bonsay.ai.agents.agent;

import com.bonsay.ai.agents.CodingState;
import com.bonsay.ai.llm.Json;
import com.bonsay.ai.llm.LlmClient;
import com.bonsay.ai.llm.LlmRequest;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/** Breaks the user task into a structured spec (class name, signature, steps, edge cases). */
public class PlannerAgent implements UnaryOperator<CodingState> {

    public static final String SYSTEM = """
            ROLE: PLANNER. You are a senior software architect.
            Turn the task into a plan for ONE self-contained Java 21 class with a single public static method.
            The class must use only java.lang and java.util. Respond with JSON only:
            {"className":"...","methodSignature":"public static ...","steps":["..."],"edgeCases":["..."]}""";

    private final LlmClient llm;

    public PlannerAgent(LlmClient llm) { this.llm = llm; }

    @Override
    public CodingState apply(CodingState s) {
        String out = llm.complete(LlmRequest.of(SYSTEM, "Task: " + s.task)).text();
        JsonNode n = Json.read(LlmOutput.extractJsonObject(out));
        String className = n.path("className").asText();
        if (!className.matches("[A-Z][A-Za-z0-9]{0,63}")) throw new IllegalStateException("planner produced invalid class name: " + className);
        s.spec = new TaskSpec(className, n.path("methodSignature").asText(), list(n.path("steps")), list(n.path("edgeCases")));
        s.log.add("planner: " + s.spec.className() + " :: " + s.spec.methodSignature());
        return s;
    }

    private static List<String> list(JsonNode arr) {
        List<String> out = new ArrayList<>();
        arr.forEach(x -> out.add(x.asText()));
        return out;
    }
}
