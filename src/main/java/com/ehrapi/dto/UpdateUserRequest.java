package com.ehrapi.dto;

/**
 * Admin request to update a user. Null fields are left unchanged; a non-blank
 * {@code password} resets the user's credentials.
 */
public record UpdateUserRequest(
        String fullName,
        Long institutionId,
        String roleCode,
        Boolean enabled,
        String password) {
}
