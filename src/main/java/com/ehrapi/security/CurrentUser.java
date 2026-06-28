package com.ehrapi.security;

import com.ehrapi.dto.CurrentUserDto;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Describes the currently authenticated principal for the web client (identity,
 * institution, role and effective permissions). Works for both local-mode and
 * OIDC-mode tokens, since both expose authorities on the {@link Authentication}
 * and carry the institution and role claims.
 */
@Component
public class CurrentUser {

    private final CurrentInstitution currentInstitution;
    private final String rolesClaim;

    public CurrentUser(CurrentInstitution currentInstitution,
                       @org.springframework.beans.factory.annotation.Value("${ehr.auth.roles-claim:roles}") String rolesClaim) {
        this.currentInstitution = currentInstitution;
        this.rolesClaim = rolesClaim;
    }

    public Optional<CurrentUserDto> describe() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }

        List<String> permissions = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).sorted().toList();

        String username = auth.getName();
        String fullName = username;
        String role = null;

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            username = jwt.getSubject() != null ? jwt.getSubject() : username;
            Object name = jwt.getClaim("name");
            if (name != null) {
                fullName = name.toString();
            }
            role = firstRole(jwt.getClaim(rolesClaim));
        }

        return Optional.of(new CurrentUserDto(
                username, fullName, currentInstitution.resolve().orElse(null), role, permissions));
    }

    private static String firstRole(Object claim) {
        if (claim instanceof java.util.Collection<?> c && !c.isEmpty()) {
            Object first = c.iterator().next();
            return first != null ? first.toString() : null;
        }
        if (claim instanceof String s && !s.isBlank()) {
            return s.split("[,\\s]+")[0];
        }
        return null;
    }
}
