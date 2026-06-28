package com.ehrapi.dto;

/** Result of a successful local-mode login. */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        CurrentUserDto user) {

    public static LoginResponse bearer(String token, long expiresInSeconds, CurrentUserDto user) {
        return new LoginResponse(token, "Bearer", expiresInSeconds, user);
    }
}
