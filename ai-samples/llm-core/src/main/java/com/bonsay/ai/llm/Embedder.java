package com.bonsay.ai.llm;

/** Turns text into a dense vector. Implementations must return L2-normalised vectors. */
public interface Embedder {
    float[] embed(String text);

    int dimensions();

    static double cosine(float[] a, float[] b) {
        if (a.length != b.length) throw new IllegalArgumentException("dimension mismatch");
        double dot = 0;
        for (int i = 0; i < a.length; i++) dot += a[i] * b[i];
        return dot; // inputs are normalised
    }
}
