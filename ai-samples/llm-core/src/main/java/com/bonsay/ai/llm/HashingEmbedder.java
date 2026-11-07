package com.bonsay.ai.llm;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

/**
 * Offline, deterministic embedder using the hashing trick over unigrams, bigrams and character
 * trigrams. Not as good as a neural embedding model, but good enough for demos and fully
 * reproducible tests. Swap in a hosted embedding model by implementing {@link Embedder}.
 */
public class HashingEmbedder implements Embedder {

    private static final Pattern NON_WORD = Pattern.compile("[^a-z0-9]+");
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "of", "to", "in", "on", "for", "and", "or", "is", "are", "was", "were",
            "be", "by", "with", "what", "how", "do", "does", "i", "we", "our", "my", "it", "this",
            "that", "at", "as", "can", "me", "you", "your", "please", "tell", "about");

    private final int dims;

    public HashingEmbedder() { this(512); }

    public HashingEmbedder(int dims) { this.dims = dims; }

    @Override
    public int dimensions() { return dims; }

    @Override
    public float[] embed(String text) {
        float[] v = new float[dims];
        List<String> tokens = tokenize(text);
        for (int i = 0; i < tokens.size(); i++) {
            String t = tokens.get(i);
            add(v, "w:" + t, 1.0f);
            if (i + 1 < tokens.size()) add(v, "b:" + t + "_" + tokens.get(i + 1), 0.7f);
            String padded = "#" + t + "#";
            for (int j = 0; j + 3 <= padded.length(); j++) add(v, "c:" + padded.substring(j, j + 3), 0.3f);
        }
        normalise(v);
        return v;
    }

    public static List<String> tokenize(String text) {
        if (text == null) return List.of();
        return NON_WORD.splitAsStream(text.toLowerCase(Locale.ROOT))
                .filter(t -> !t.isBlank() && !STOP_WORDS.contains(t))
                .map(HashingEmbedder::stem)
                .toList();
    }

    /** Very small suffix stripper so "policies" and "policy" collide. */
    static String stem(String t) {
        if (t.length() > 4 && t.endsWith("ies")) return t.substring(0, t.length() - 3) + "y";
        if (t.length() > 3 && t.endsWith("s") && !t.endsWith("ss")) return t.substring(0, t.length() - 1);
        return t;
    }

    private void add(float[] v, String feature, float weight) {
        CRC32 crc = new CRC32();
        crc.update(feature.getBytes(StandardCharsets.UTF_8));
        long h = crc.getValue();
        int idx = (int) (h % dims);
        float sign = ((h >>> 31) & 1) == 0 ? 1f : -1f;
        v[idx] += sign * weight;
    }

    private static void normalise(float[] v) {
        double norm = 0;
        for (float x : v) norm += x * x;
        norm = Math.sqrt(norm);
        if (norm == 0) return;
        for (int i = 0; i < v.length; i++) v[i] = (float) (v[i] / norm);
    }
}
