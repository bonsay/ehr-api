package com.bonsay.ai.rag.retrieval;

import com.bonsay.ai.llm.Embedder;
import com.bonsay.ai.rag.ingest.Chunk;
import com.bonsay.ai.rag.ingest.Chunker;
import com.bonsay.ai.rag.ingest.SourceDocument;
import com.bonsay.ai.rag.security.AccessPolicy;
import com.bonsay.ai.rag.security.UserPrincipal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory vector index with <b>pre-retrieval ACL filtering</b>: chunks the user may not read
 * are excluded before similarity ranking, so they can never reach the prompt, the logs or the
 * answer. (Post-filtering top-k would also leak via "fewer results than expected" side channels.)
 * Replace with pgvector / OpenSearch using a metadata filter on {@code allowed_roles}.
 */
public class VectorStore {

    private final Embedder embedder;
    private final Chunker chunker;
    private final List<Chunk> chunks = new CopyOnWriteArrayList<>();

    public VectorStore(Embedder embedder, Chunker chunker) {
        this.embedder = embedder;
        this.chunker = chunker;
    }

    public void ingest(SourceDocument doc) {
        List<String> parts = chunker.split(doc.text());
        for (int i = 0; i < parts.size(); i++) {
            String text = parts.get(i);
            // Title is prepended for embedding only - improves recall for short chunks.
            float[] vec = embedder.embed(doc.title() + ". " + text);
            chunks.add(new Chunk(doc.id() + "#" + i, doc.id(), doc.title(), doc.allowedRoles(), text, vec));
        }
    }

    public List<ScoredChunk> search(UserPrincipal user, String query, int k, double minScore) {
        float[] q = embedder.embed(query);
        List<ScoredChunk> scored = new ArrayList<>();
        for (Chunk c : chunks) {
            if (!AccessPolicy.canRead(user, c.allowedRoles())) continue;   // RBAC first
            double s = Embedder.cosine(q, c.vector());
            if (s >= minScore) scored.add(new ScoredChunk(c, s));
        }
        scored.sort(Comparator.comparingDouble(ScoredChunk::score).reversed());
        return scored.subList(0, Math.min(k, scored.size()));
    }

    public int size() { return chunks.size(); }

    public record ScoredChunk(Chunk chunk, double score) {}
}
