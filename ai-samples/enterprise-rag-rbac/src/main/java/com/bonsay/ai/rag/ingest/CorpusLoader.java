package com.bonsay.ai.rag.ingest;

import com.bonsay.ai.llm.Json;
import com.bonsay.ai.rag.security.Role;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/** Loads the sample corpus from {@code corpus.json} on the classpath. */
public final class CorpusLoader {
    private CorpusLoader() {}

    public static List<SourceDocument> loadDefault() {
        try (InputStream in = CorpusLoader.class.getResourceAsStream("/corpus.json")) {
            if (in == null) throw new IllegalStateException("corpus.json not found");
            JsonNode root = Json.read(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            List<SourceDocument> docs = new ArrayList<>();
            for (JsonNode d : root) {
                EnumSet<Role> roles = EnumSet.noneOf(Role.class);
                for (JsonNode r : d.path("allowedRoles")) roles.add(Role.valueOf(r.asText()));
                docs.add(new SourceDocument(d.path("id").asText(), d.path("title").asText(),
                        d.path("department").asText(), roles, d.path("text").asText()));
            }
            return docs;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
