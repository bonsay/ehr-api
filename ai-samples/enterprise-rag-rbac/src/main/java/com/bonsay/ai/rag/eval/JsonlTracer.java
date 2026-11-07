package com.bonsay.ai.rag.eval;

import com.bonsay.ai.llm.Json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Appends one JSON object per run to a local file - grep-able audit log of every answer. */
public class JsonlTracer implements Tracer {
    private final Path file;

    public JsonlTracer(Path file) {
        this.file = file;
    }

    @Override
    public synchronized void trace(String name, Map<String, Object> inputs, Object outputs) {
        Map<String, Object> rec = new LinkedHashMap<>();
        rec.put("id", UUID.randomUUID().toString());
        rec.put("ts", Instant.now().toString());
        rec.put("name", name);
        rec.put("inputs", inputs);
        rec.put("outputs", outputs);
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            Files.writeString(file, Json.write(rec) + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("[trace] failed to write: " + e.getMessage());
        }
    }
}
