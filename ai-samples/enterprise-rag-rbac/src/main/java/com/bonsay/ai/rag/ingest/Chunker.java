package com.bonsay.ai.rag.ingest;

import java.util.ArrayList;
import java.util.List;

/** Sentence-aware sliding-window chunker (window and overlap measured in words). */
public class Chunker {
    private final int maxWords;
    private final int overlapWords;

    public Chunker(int maxWords, int overlapWords) {
        if (overlapWords >= maxWords) throw new IllegalArgumentException("overlap must be < window");
        this.maxWords = maxWords;
        this.overlapWords = overlapWords;
    }

    public List<String> split(String text) {
        String[] sentences = text.trim().split("(?<=[.!?])\\s+");
        List<String> chunks = new ArrayList<>();
        List<String> window = new ArrayList<>();
        for (String sentence : sentences) {
            List<String> words = List.of(sentence.split("\\s+"));
            if (!window.isEmpty() && window.size() + words.size() > maxWords) {
                chunks.add(String.join(" ", window));
                window = new ArrayList<>(window.subList(Math.max(0, window.size() - overlapWords), window.size()));
            }
            window.addAll(words);
        }
        if (!window.isEmpty()) chunks.add(String.join(" ", window));
        return chunks;
    }
}
