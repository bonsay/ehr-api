package com.ehrapi.security;

import com.ehrapi.entity.Role;
import com.ehrapi.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Expands role codes into the concrete permission authorities they grant, by
 * reading the (admin-editable) role definitions from the database. This is the
 * single place where "what can this role do" is answered, so both local-mode
 * login and OIDC-mode token mapping stay consistent with the admin console.
 */
@Service
public class RoleAuthorityService {

    private final RoleRepository roles;

    public RoleAuthorityService(RoleRepository roles) {
        this.roles = roles;
    }

    /** All permissions granted by the given role codes (deduplicated). */
    public Set<String> permissionsFor(List<String> roleCodes) {
        Set<String> permissions = new LinkedHashSet<>();
        if (roleCodes == null) {
            return permissions;
        }
        for (String code : roleCodes) {
            if (code == null || code.isBlank()) {
                continue;
            }
            roles.findByCode(code.trim()).map(Role::getPermissions).ifPresent(permissions::addAll);
        }
        return permissions;
    }

    /** Convenience for a single role. */
    public Set<String> permissionsFor(String roleCode) {
        return permissionsFor(List.of(roleCode));
    }
}
