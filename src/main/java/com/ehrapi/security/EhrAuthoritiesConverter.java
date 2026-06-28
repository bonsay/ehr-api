package com.ehrapi.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Derives Spring Security authorities from a validated JWT.
 *
 * <p>Two claim shapes are supported and combined:
 * <ul>
 *   <li><b>authorities</b> — permission strings carried directly on the token
 *       (used by local-mode tokens, which embed the user's resolved permissions);</li>
 *   <li><b>roles</b> — role codes that are expanded into permissions via the
 *       admin-editable {@link RoleAuthorityService} (used in OIDC mode, where the
 *       identity provider asserts roles and the platform owns the role→permission
 *       mapping).</li>
 * </ul>
 */
public class EhrAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final String authoritiesClaim;
    private final String rolesClaim;
    private final RoleAuthorityService roleAuthorityService;

    public EhrAuthoritiesConverter(String authoritiesClaim, String rolesClaim,
                                   RoleAuthorityService roleAuthorityService) {
        this.authoritiesClaim = authoritiesClaim;
        this.rolesClaim = rolesClaim;
        this.roleAuthorityService = roleAuthorityService;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<String> permissions = new LinkedHashSet<>();

        permissions.addAll(asStringList(jwt.getClaim(authoritiesClaim)));

        List<String> roleCodes = asStringList(jwt.getClaim(rolesClaim));
        if (roleAuthorityService != null && !roleCodes.isEmpty()) {
            permissions.addAll(roleAuthorityService.permissionsFor(roleCodes));
        }

        return permissions.stream()
                .map(p -> (GrantedAuthority) new SimpleGrantedAuthority(p))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static List<String> asStringList(Object claim) {
        if (claim instanceof Collection<?> c) {
            return c.stream().filter(java.util.Objects::nonNull).map(Object::toString).toList();
        }
        if (claim instanceof String s && !s.isBlank()) {
            return List.of(s.split("[,\\s]+"));
        }
        return List.of();
    }
}
