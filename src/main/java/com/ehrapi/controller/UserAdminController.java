package com.ehrapi.controller;

import com.ehrapi.dto.CreateUserRequest;
import com.ehrapi.dto.UpdateUserRequest;
import com.ehrapi.dto.UserDto;
import com.ehrapi.service.UserAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Administrative management of users and their role assignments. Gated by the
 * {@code ADMIN:USERS} permission — i.e. the administrator role.
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasAuthority('ADMIN:USERS')")
@Tag(name = "Admin: Users", description = "Provision users and assign roles")
public class UserAdminController {

    private final UserAdminService service;

    public UserAdminController(UserAdminService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all users")
    public List<UserDto> list() {
        return service.list();
    }

    @PostMapping
    @Operation(summary = "Create a user and assign a role")
    public ResponseEntity<UserDto> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a user (role, status, name, password)")
    public UserDto update(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
