package com.bonsay.ai.llm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Deterministic test double. Either answers via a function of the request, or replays a fixed
 * script of responses in order. Records every request so tests can assert on prompts.
 */
public class ScriptedLlmClient implements LlmClient {

    private final Function<LlmRequest, String> responder;
    private final List<LlmRequest> requests = Collections.synchronizedList(new ArrayList<>());

    public ScriptedLlmClient(Function<LlmRequest, String> responder) {
        this.responder = responder;
    }

    /** Replays responses in order; the last response repeats once the script is exhausted. */
    public static ScriptedLlmClient sequence(String... responses) {
        if (responses.length == 0) throw new IllegalArgumentException("at least one response required");
        int[] cursor = {0};
        return new ScriptedLlmClient(req -> {
            synchronized (cursor) {
                String r = responses[Math.min(cursor[0], responses.length - 1)];
                cursor[0]++;
                return r;
            }
        });
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        requests.add(request);
        String text = responder.apply(request);
        String model = request.model() == null ? "scripted" : request.model();
        int in = TokenEstimator.estimate(request.system()) + request.messages().stream()
                .mapToInt(m -> TokenEstimator.estimate(m.content())).sum();
        return new LlmResponse(text, model, in, TokenEstimator.estimate(text), "end_turn");
    }

    public List<LlmRequest> requests() {
        return List.copyOf(requests);
    }

    public int callCount() {
        return requests.size();
    }
}
