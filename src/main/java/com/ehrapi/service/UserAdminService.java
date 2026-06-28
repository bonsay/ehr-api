package com.ehrapi.service;

import com.ehrapi.dto.CreateUserRequest;
import com.ehrapi.dto.UpdateUserRequest;
import com.ehrapi.dto.UserDto;
import com.ehrapi.entity.AppUser;
import com.ehrapi.exception.ResourceNotFoundException;
import com.ehrapi.repository.AppUserRepository;
import com.ehrapi.repository.RoleRepository;
import com.ehrapi.security.Permissions;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Administrative management of users — provisioning accounts and assigning the
 * role that determines what each user may do. Guards against locking the
 * platform out of administration by removing the last active administrator.
 */
@Service
public class UserAdminService {

    private final AppUserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;

    public UserAdminService(AppUserRepository users, RoleRepository roles, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserDto> list() {
        return users.findAll().stream()
                .sorted((a, b) -> a.getUsername().compareTo(b.getUsername()))
                .map(UserDto::from).toList();
    }

    @Transactional
    public UserDto create(CreateUserRequest req) {
        if (users.existsByUsername(req.username())) {
            throw new IllegalArgumentException("Username already exists: " + req.username());
        }
        requireRole(req.roleCode());

        AppUser user = new AppUser();
        user.setUsername(req.username());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setFullName(req.fullName());
        user.setInstitutionId(req.institutionId());
        user.setRoleCode(req.roleCode());
        user.setEnabled(true);
        return UserDto.from(users.save(user));
    }

    @Transactional
    public UserDto update(Long id, UpdateUserRequest req) {
        AppUser user = users.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        boolean losingAdmin = false;
        if (req.roleCode() != null && !req.roleCode().equals(user.getRoleCode())) {
            requireRole(req.roleCode());
            losingAdmin = Permissions.ROLE_ADMINISTRATOR.equals(user.getRoleCode());
        }
        boolean beingDisabled = Boolean.FALSE.equals(req.enabled()) && user.isEnabled()
                && Permissions.ROLE_ADMINISTRATOR.equals(user.getRoleCode());
        if (losingAdmin || beingDisabled) {
            ensureNotLastAdministrator(user.getId());
        }

        if (req.fullName() != null) user.setFullName(req.fullName());
        if (req.institutionId() != null) user.setInstitutionId(req.institutionId());
        if (req.roleCode() != null) user.setRoleCode(req.roleCode());
        if (req.enabled() != null) user.setEnabled(req.enabled());
        if (req.password() != null && !req.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(req.password()));
        }
        return UserDto.from(users.save(user));
    }

    @Transactional
    public void delete(Long id) {
        AppUser user = users.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        if (Permissions.ROLE_ADMINISTRATOR.equals(user.getRoleCode())) {
            ensureNotLastAdministrator(user.getId());
        }
        users.delete(user);
    }

    private void requireRole(String code) {
        if (!roles.existsByCode(code)) {
            throw new IllegalArgumentException("Unknown role: " + code);
        }
    }

    /** Refuse to remove/demote the final enabled administrator. */
    private void ensureNotLastAdministrator(Long excludingUserId) {
        long remainingAdmins = users.findAll().stream()
                .filter(u -> Permissions.ROLE_ADMINISTRATOR.equals(u.getRoleCode()))
                .filter(AppUser::isEnabled)
                .filter(u -> !u.getId().equals(excludingUserId))
                .count();
        if (remainingAdmins == 0) {
            throw new IllegalArgumentException(
                    "Cannot remove the last active administrator.");
        }
    }
}
