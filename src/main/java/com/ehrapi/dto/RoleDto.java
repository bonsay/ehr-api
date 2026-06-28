package com.ehrapi.dto;

import com.ehrapi.entity.Role;

import java.util.List;

/** A role and the permissions it grants, for the admin console. */
public record RoleDto(
        String code,
        String name,
        String description,
        boolean systemRole,
        List<String> permissions) {

    public static RoleDto from(Role role) {
        return new RoleDto(role.getCode(), role.getName(), role.getDescription(),
                role.isSystemRole(), List.copyOf(role.getPermissions()));
    }
}
