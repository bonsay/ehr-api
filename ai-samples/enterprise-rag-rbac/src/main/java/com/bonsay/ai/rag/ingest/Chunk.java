package com.bonsay.ai.rag.ingest;

import com.bonsay.ai.rag.security.Role;

import java.util.Set;

/** A retrievable slice of a document. ACLs are copied onto every chunk so filtering is local. */
public record Chunk(String id, String documentId, String title, Set<Role> allowedRoles, String text, float[] vector) {
}
