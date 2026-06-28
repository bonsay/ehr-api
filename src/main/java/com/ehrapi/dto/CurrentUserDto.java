package com.ehrapi.dto;

import java.util.List;

/**
 * The authenticated principal as the web client needs it: identity, the
 * institution they act for, their role, and the flat permission list that drives
 * UI gating. Never carries credentials.
 */
public record CurrentUserDto(
        String username,
        String fullName,
        Long institutionId,
        String role,
        List<String> permissions) {
}
