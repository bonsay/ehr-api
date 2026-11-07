package com.bonsay.ai.llm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HashingEmbedderTest {

    private final HashingEmbedder embedder = new HashingEmbedder();

    @Test
    void similarTextsScoreHigherThanUnrelatedTexts() {
        float[] q = embedder.embed("What is the travel expense reimbursement policy?");
        float[] related = embedder.embed("Travel expenses are reimbursed within 30 days per policy.");
        float[] unrelated = embedder.embed("The Kubernetes cluster autoscaler adds nodes under load.");
        assertTrue(Embedder.cosine(q, related) > Embedder.cosine(q, unrelated));
    }

    @Test
    void vectorsAreNormalisedAndDeterministic() {
        float[] a = embedder.embed("hello world");
        float[] b = embedder.embed("hello world");
        assertArrayEquals(a, b);
        assertEquals(1.0, Embedder.cosine(a, a), 1e-5);
    }

    @Test
    void scriptedClientReplaysInOrderAndRecordsRequests() {
        ScriptedLlmClient client = ScriptedLlmClient.sequence("one", "two");
        assertEquals("one", client.complete(LlmRequest.of("", "a")).text());
        assertEquals("two", client.complete(LlmRequest.of("", "b")).text());
        assertEquals("two", client.complete(LlmRequest.of("", "c")).text());
        assertEquals(3, client.callCount());
        assertEquals("b", client.requests().get(1).lastUserMessage());
    }
}
