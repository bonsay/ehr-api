package com.ehrapi.service;

import com.ehrapi.dto.CurrentUserDto;
import com.ehrapi.dto.LoginResponse;
import com.ehrapi.entity.AppUser;
import com.ehrapi.repository.AppUserRepository;
import com.ehrapi.security.LocalTokenService;
import com.ehrapi.security.RoleAuthorityService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Local-mode authentication: verifies a username/password against the user store
 * and issues a signed access token embedding the user's institution, role and
 * resolved permissions. Only present in {@code local} security mode.
 */
@Service
@ConditionalOnProperty(name = "ehr.security.mode", havingValue = "local")
public class LoginService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final RoleAuthorityService roleAuthorityService;
    private final LocalTokenService tokenService;

    public LoginService(AppUserRepository users, PasswordEncoder passwordEncoder,
                        RoleAuthorityService roleAuthorityService, LocalTokenService tokenService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.roleAuthorityService = roleAuthorityService;
        this.tokenService = tokenService;
    }

    public LoginResponse login(String username, String rawPassword) {
        AppUser user = users.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password."));
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password.");
        }
        if (!user.isEnabled()) {
            throw new DisabledException("This account is disabled.");
        }

        List<String> permissions = List.copyOf(roleAuthorityService.permissionsFor(user.getRoleCode()));
        var issued = tokenService.issue(user.getUsername(), user.getFullName(),
                user.getInstitutionId(), user.getRoleCode(), permissions);

        CurrentUserDto dto = new CurrentUserDto(user.getUsername(), user.getFullName(),
                user.getInstitutionId(), user.getRoleCode(), permissions);
        return LoginResponse.bearer(issued.token(), issued.expiresInSeconds(), dto);
    }
}
