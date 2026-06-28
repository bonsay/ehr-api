package com.ehrapi.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;

/**
 * Mints short-lived HS256 access tokens for {@code local} security mode. Tokens
 * embed the user's institution (consumed by {@link CurrentInstitution}), their
 * role and their fully-resolved permission list (consumed by
 * {@link EhrAuthoritiesConverter}), so the resource-server filter can authorize
 * requests without a round-trip to the database.
 *
 * <p>Only created in local mode; the {@link JwtEncoder} it depends on is defined
 * by {@code LocalSecurityConfig}.
 */
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "ehr.security.mode", havingValue = "local")
public class LocalTokenService {

    private final JwtEncoder encoder;
    private final String institutionClaim;
    private final long ttlSeconds;
    private final String issuer;

    public LocalTokenService(JwtEncoder encoder,
                             @Value("${ehr.auth.institution-claim:institution_id}") String institutionClaim,
                             @Value("${ehr.security.local.token-ttl-seconds:28800}") long ttlSeconds) {
        this.encoder = encoder;
        this.institutionClaim = institutionClaim;
        this.ttlSeconds = ttlSeconds;
        this.issuer = "ehr-api";
    }

    public IssuedToken issue(String username, String fullName, Long institutionId,
                             String roleCode, Collection<String> authorities) {
        Instant now = Instant.now();
        Instant expiry = now.plus(ttlSeconds, ChronoUnit.SECONDS);

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(expiry)
                .subject(username)
                .claim("name", fullName == null ? username : fullName)
                .claim("roles", List.of(roleCode))
                .claim("authorities", List.copyOf(authorities));
        if (institutionId != null) {
            claims.claim(institutionClaim, institutionId);
        }

        var header = JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new IssuedToken(token, expiry, ttlSeconds);
    }

    public record IssuedToken(String token, Instant expiresAt, long expiresInSeconds) {}
}
