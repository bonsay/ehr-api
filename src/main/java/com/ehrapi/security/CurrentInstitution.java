package com.ehrapi.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Resolves the caller's institution id from the authenticated JWT, reading a
 * configurable claim. This is the trust anchor for cross-institution sharing:
 * the requesting institution is taken from the verified token, never from a
 * client-supplied parameter.
 *
 * <p>In open/local-dev mode there is no token, so callers fall back to an
 * explicit parameter via {@link #resolveOrFallback(Long)}.
 */
@Component
public class CurrentInstitution {

    private final String institutionClaim;

    public CurrentInstitution(
            @Value("${ehr.auth.institution-claim:institution_id}") String institutionClaim) {
        this.institutionClaim = institutionClaim;
    }

    /** The institution id from the current JWT, if authenticated with one. */
    public Optional<Long> resolve() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return parse(jwtAuth.getToken().getClaims().get(institutionClaim));
        }
        return Optional.empty();
    }

    /**
     * The institution from the token when present (secured mode); otherwise the
     * supplied fallback (open/dev mode). Throws when neither is available.
     */
    public Long resolveOrFallback(Long fallback) {
        return resolve().orElseGet(() -> {
            if (fallback == null) {
                throw new IllegalArgumentException(
                        "Requesting institution could not be determined from the access token.");
            }
            return fallback;
        });
    }

    private static Optional<Long> parse(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            if (value instanceof Number number) {
                return Optional.of(number.longValue());
            }
            return Optional.of(Long.parseLong(value.toString().trim()));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
