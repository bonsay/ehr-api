package com.ehrapi.dto;

import jakarta.validation.constraints.NotBlank;

/** Credentials for local-mode authentication. */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password) {
}
