package com.ehrapi.dto;

import java.util.List;

/**
 * Outcome of self-service onboarding: the new institution, the free modules it
 * was provisioned with, and — in local security mode — a ready-to-use access
 * token so the new admin is signed straight in.
 */
public record RegistrationResult(
        Long institutionId,
        String institutionCode,
        String adminUsername,
        List<String> enabledModules,
        /** Auto-login token (local mode); null when auth is via an external IdP. */
        LoginResponse login) {
}
