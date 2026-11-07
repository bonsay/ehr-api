package com.bonsay.ai.rag.ingest;

import com.bonsay.ai.rag.security.Role;

import java.util.Set;

/** A document as ingested from a source system, carrying its ACL. */
public record SourceDocument(String id, String title, String department, Set<Role> allowedRoles, String text) {
    public SourceDocument {
        allowedRoles = Set.copyOf(allowedRoles);
    }
}
