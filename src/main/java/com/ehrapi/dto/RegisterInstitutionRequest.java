package com.ehrapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Self-service sign-up: provision a new institution and its first administrator
 * in one step. Public (no authentication) so a prospective customer can start a
 * free-tier workspace without an existing account.
 */
public record RegisterInstitutionRequest(
        @NotBlank String institutionName,
        @NotBlank String institutionCode,
        @NotBlank String adminUsername,
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters.") String adminPassword,
        String adminFullName) {
}
