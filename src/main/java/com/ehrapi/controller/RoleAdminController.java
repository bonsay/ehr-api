package com.ehrapi.controller;

import com.ehrapi.dto.RoleDto;
import com.ehrapi.dto.UpdateRolePermissionsRequest;
import com.ehrapi.service.RoleAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Administrative management of roles and the permissions they grant. Gated by
 * the {@code ADMIN:ROLES} permission — i.e. the administrator role.
 */
@RestController
@RequestMapping("/api/admin/roles")
@PreAuthorize("hasAuthority('ADMIN:ROLES')")
@Tag(name = "Admin: Roles", description = "Manage roles and their module permissions")
public class RoleAdminController {

    private final RoleAdminService service;

    public RoleAdminController(RoleAdminService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all roles with their permissions")
    public List<RoleDto> list() {
        return service.list();
    }

    @GetMapping("/permission-catalog")
    @Operation(summary = "The catalog of assignable permissions, grouped")
    public Map<String, List<String>> permissionCatalog() {
        return service.permissionCatalog();
    }

    @PutMapping("/{code}/permissions")
    @Operation(summary = "Replace the permissions granted by a role")
    public RoleDto updatePermissions(@PathVariable String code,
                                     @RequestBody UpdateRolePermissionsRequest request) {
        return service.updatePermissions(code, request.permissions());
    }
}
