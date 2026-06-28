package com.ehrapi.security;

import com.ehrapi.entity.AppUser;
import com.ehrapi.entity.Role;
import com.ehrapi.repository.AppUserRepository;
import com.ehrapi.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the built-in roles and (optionally) a set of demo users on startup.
 *
 * <p>Roles are seeded idempotently in every environment so authorization has a
 * baseline to resolve against. Demo users are seeded only when
 * {@code ehr.security.seed-demo-users} is true (default for the {@code h2}
 * profile) — they make the role model immediately demonstrable without an
 * external identity provider.
 */
@Component
public class SecuritySeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SecuritySeeder.class);

    private final RoleRepository roles;
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final boolean seedDemoUsers;

    public SecuritySeeder(RoleRepository roles, AppUserRepository users, PasswordEncoder passwordEncoder,
                          @Value("${ehr.security.seed-demo-users:false}") boolean seedDemoUsers) {
        this.roles = roles;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.seedDemoUsers = seedDemoUsers;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedRoles();
        if (seedDemoUsers) {
            seedDemoUsers();
        }
    }

    private void seedRoles() {
        for (String code : Permissions.builtInRoles()) {
            if (!roles.existsByCode(code)) {
                Role role = new Role(
                        code,
                        prettyName(code),
                        Permissions.descriptionFor(code),
                        true,
                        Permissions.defaultsFor(code));
                roles.save(role);
                log.info("Seeded built-in role {}", code);
            }
        }
    }

    /** Demo accounts at General Hospital (id 1) and Downtown Clinic (id 2). */
    private void seedDemoUsers() {
        createUserIfAbsent("admin", "admin123", "System Administrator", 1L, Permissions.ROLE_ADMINISTRATOR);
        createUserIfAbsent("physician", "physician123", "Dr. Alice Adams", 1L, Permissions.ROLE_PHYSICIAN);
        createUserIfAbsent("nurse", "nurse123", "Nina Nurse", 1L, Permissions.ROLE_NURSE);
        createUserIfAbsent("reception", "reception123", "Riley Reception", 2L, Permissions.ROLE_RECEPTIONIST);
    }

    private void createUserIfAbsent(String username, String rawPassword, String fullName,
                                    Long institutionId, String roleCode) {
        if (users.existsByUsername(username)) {
            return;
        }
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName);
        user.setInstitutionId(institutionId);
        user.setRoleCode(roleCode);
        user.setEnabled(true);
        users.save(user);
        log.info("Seeded demo user '{}' with role {}", username, roleCode);
    }

    private static String prettyName(String code) {
        String lower = code.toLowerCase().replace('_', ' ');
        List<String> words = List.of(lower.split(" "));
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }
}
