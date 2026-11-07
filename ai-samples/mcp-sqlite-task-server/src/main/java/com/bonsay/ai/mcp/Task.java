package com.bonsay.ai.mcp;

/** A row in the {@code tasks} table. */
public record Task(long id, String title, String description, String status, String priority,
                   String dueDate, String createdAt, String completedAt) {
}
