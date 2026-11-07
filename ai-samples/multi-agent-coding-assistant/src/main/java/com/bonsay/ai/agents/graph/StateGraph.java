package com.bonsay.ai.agents.graph;

import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * A tiny LangGraph-style orchestrator: named nodes transform a shared state, and edges
 * (static or conditional) decide which node runs next until {@link #END} is reached.
 * A step budget guards against infinite agent loops.
 *
 * @param <S> the shared state type
 */
public class StateGraph<S> {

    public static final String END = "__end__";

    private final Map<String, UnaryOperator<S>> nodes = new LinkedHashMap<>();
    private final Map<String, Function<S, String>> edges = new HashMap<>();
    private String entryPoint;

    public StateGraph<S> addNode(String name, UnaryOperator<S> node) {
        if (END.equals(name)) throw new IllegalArgumentException("reserved node name");
        if (nodes.putIfAbsent(name, node) != null) throw new IllegalArgumentException("duplicate node " + name);
        return this;
    }

    public StateGraph<S> addEdge(String from, String to) {
        return addConditionalEdge(from, s -> to);
    }

    /** {@code router} returns the next node name (or {@link #END}) based on the state. */
    public StateGraph<S> addConditionalEdge(String from, Function<S, String> router) {
        edges.put(from, router);
        return this;
    }

    public StateGraph<S> setEntryPoint(String name) {
        this.entryPoint = name;
        return this;
    }

    public Compiled<S> compile() {
        if (entryPoint == null || !nodes.containsKey(entryPoint)) throw new IllegalStateException("invalid entry point");
        for (String n : nodes.keySet()) {
            if (!edges.containsKey(n)) throw new IllegalStateException("node '" + n + "' has no outgoing edge");
        }
        return new Compiled<>(Map.copyOf(nodes), Map.copyOf(edges), entryPoint);
    }

    /** Observes each transition (for logging / tracing / UI streaming). */
    public interface Listener<S> {
        void onStep(int step, String node, S state);
    }

    public record Compiled<S>(Map<String, UnaryOperator<S>> nodes, Map<String, Function<S, String>> edges, String entry) {

        public Result<S> invoke(S initial, int maxSteps, Listener<S> listener) {
            S state = initial;
            String current = entry;
            List<String> path = new ArrayList<>();
            for (int step = 1; step <= maxSteps; step++) {
                UnaryOperator<S> node = nodes.get(current);
                if (node == null) throw new IllegalStateException("unknown node: " + current);
                path.add(current);
                state = node.apply(state);
                listener.onStep(step, current, state);
                String next = edges.get(current).apply(state);
                if (END.equals(next)) return new Result<>(state, path, false);
                current = next;
            }
            return new Result<>(state, path, true);
        }

        public Result<S> invoke(S initial, int maxSteps) {
            return invoke(initial, maxSteps, (i, n, s) -> {});
        }
    }

    public record Result<S>(S state, List<String> path, boolean stepBudgetExhausted) {}
}
