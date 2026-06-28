package com.ehrapi.service;

import com.ehrapi.dto.RoleDto;
import com.ehrapi.entity.Role;
import com.ehrapi.exception.ResourceNotFoundException;
import com.ehrapi.repository.RoleRepository;
import com.ehrapi.security.Permissions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Administrative management of roles and their permissions. The set of
 * assignable permissions is fixed by the platform's {@link Permissions} catalog;
 * which of them each role carries is what an administrator controls.
 */
@Service
public class RoleAdminService {

    private final RoleRepository roles;
    private final Set<String> assignablePermissions;

    public RoleAdminService(RoleRepository roles) {
        this.roles = roles;
        this.assignablePermissions = Permissions.catalog().values().stream()
                .flatMap(List::stream).collect(Collectors.toUnmodifiableSet());
    }

    public List<RoleDto> list() {
        return roles.findAll().stream()
                .sorted((a, b) -> a.getCode().compareTo(b.getCode()))
                .map(RoleDto::from).toList();
    }

    /** The catalog of assignable permissions, grouped for the admin UI. */
    public Map<String, List<String>> permissionCatalog() {
        return Permissions.catalog();
    }

    @Transactional
    public RoleDto updatePermissions(String code, List<String> permissions) {
        Role role = roles.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + code));

        Set<String> requested = new LinkedHashSet<>(permissions == null ? List.of() : permissions);
        List<String> unknown = requested.stream()
                .filter(p -> !assignablePermissions.contains(p)).toList();
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("Unknown permission(s): " + String.join(", ", unknown));
        }

        // Guard: the administrator role must retain the ability to manage roles,
        // otherwise the platform could be locked out of its own admin surface.
        if (Permissions.ROLE_ADMINISTRATOR.equals(code) && !requested.contains(Permissions.ADMIN_ROLES)) {
            throw new IllegalArgumentException(
                    "The ADMINISTRATOR role must keep the ADMIN:ROLES permission.");
        }

        role.setPermissions(requested);
        return RoleDto.from(roles.save(role));
    }
}
