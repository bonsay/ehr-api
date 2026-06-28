package com.ehrapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Admin request to provision a new user and assign their role. */
public record CreateUserRequest(
        @NotBlank String username,
        @NotBlank String password,
        String fullName,
        Long institutionId,
        @NotNull String roleCode) {
}
