package com.ehrapi.controller;

import com.ehrapi.dto.CurrentUserDto;
import com.ehrapi.dto.LoginRequest;
import com.ehrapi.dto.LoginResponse;
import com.ehrapi.security.CurrentUser;
import com.ehrapi.service.LoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication surface. In {@code local} security mode {@code POST /login}
 * verifies credentials and returns a signed access token; {@code GET /me} reports
 * the authenticated principal and their permissions (used by the web client to
 * drive role-based UI gating) in any secured mode.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Login and current-user info")
public class AuthController {

    private final ObjectProvider<LoginService> loginService;
    private final CurrentUser currentUser;

    public AuthController(ObjectProvider<LoginService> loginService, CurrentUser currentUser) {
        this.loginService = loginService;
        this.currentUser = currentUser;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate with username/password (local mode)")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        LoginService service = loginService.getIfAvailable();
        if (service == null) {
            throw new IllegalArgumentException(
                    "Password login is not enabled in this environment (it uses external OIDC).");
        }
        return service.login(request.username(), request.password());
    }

    @GetMapping("/me")
    @Operation(summary = "Describe the authenticated user and their permissions")
    public ResponseEntity<CurrentUserDto> me() {
        return currentUser.describe()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(401).build());
    }
}
