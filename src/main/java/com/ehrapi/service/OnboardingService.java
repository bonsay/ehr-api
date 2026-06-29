package com.ehrapi.service;

import com.ehrapi.dto.RegisterInstitutionRequest;
import com.ehrapi.dto.RegistrationResult;
import com.ehrapi.entity.AppUser;
import com.ehrapi.entity.EhrModule;
import com.ehrapi.entity.Institution;
import com.ehrapi.entity.InstitutionModule;
import com.ehrapi.entity.ModuleTier;
import com.ehrapi.repository.AppUserRepository;
import com.ehrapi.repository.EhrModuleRepository;
import com.ehrapi.repository.InstitutionModuleRepository;
import com.ehrapi.repository.InstitutionRepository;
import com.ehrapi.security.Permissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Self-service onboarding: stands up a new institution on the free tier in a
 * single step — create the institution, enable every FREE module, and provision
 * its first administrator. The adoption on-ramp; paid modules are added later
 * from the marketplace.
 *
 * <p>In {@code local} security mode the new admin is signed straight in (a token
 * is returned); in {@code oidc} mode users come from the external IdP, so only
 * the institution and its free modules are provisioned.
 */
@Service
public class OnboardingService {

    private static final Logger log = LoggerFactory.getLogger(OnboardingService.class);

    private final InstitutionRepository institutions;
    private final EhrModuleRepository modules;
    private final InstitutionModuleRepository institutionModules;
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<LoginService> loginService;
    private final boolean enabled;

    public OnboardingService(InstitutionRepository institutions, EhrModuleRepository modules,
                             InstitutionModuleRepository institutionModules, AppUserRepository users,
                             PasswordEncoder passwordEncoder, ObjectProvider<LoginService> loginService,
                             @Value("${ehr.onboarding.enabled:true}") boolean enabled) {
        this.institutions = institutions;
        this.modules = modules;
        this.institutionModules = institutionModules;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.loginService = loginService;
        this.enabled = enabled;
    }

    @Transactional
    public RegistrationResult register(RegisterInstitutionRequest req) {
        if (!enabled) {
            throw new IllegalStateException("Self-service registration is disabled in this environment.");
        }
        String code = req.institutionCode().trim();
        String username = req.adminUsername().trim();
        if (institutions.findByCode(code).isPresent()) {
            throw new IllegalArgumentException("Institution code already in use: " + code);
        }
        if (users.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        Institution institution = new Institution();
        institution.setName(req.institutionName().trim());
        institution.setCode(code);
        institution.setType("PRACTICE");
        institution.setActive(true);
        institution = institutions.save(institution);
        Long institutionId = institution.getId();

        List<String> freeModules = enableFreeModules(institutionId);

        // Provision the first administrator. Only meaningful where the API owns
        // the user store (local mode); LoginService is absent under external OIDC.
        boolean localMode = loginService.getIfAvailable() != null;
        if (localMode) {
            AppUser admin = new AppUser();
            admin.setUsername(username);
            admin.setPasswordHash(passwordEncoder.encode(req.adminPassword()));
            admin.setFullName(req.adminFullName());
            admin.setInstitutionId(institutionId);
            admin.setRoleCode(Permissions.ROLE_ADMINISTRATOR);
            admin.setEnabled(true);
            users.save(admin);
        }

        log.info("Onboarded institution {} (id {}) with {} free modules; localAdmin={}",
                code, institutionId, freeModules.size(), localMode);

        var login = loginService.getIfAvailable();
        return new RegistrationResult(institutionId, code, username, freeModules,
                login != null ? login.login(username, req.adminPassword()) : null);
    }

    /** Enable every FREE-tier catalog module for the institution. */
    private List<String> enableFreeModules(Long institutionId) {
        List<EhrModule> free = modules.findAll().stream()
                .filter(m -> m.getTier() == ModuleTier.FREE)
                .toList();
        for (EhrModule m : free) {
            institutionModules.save(new InstitutionModule(institutionId, m.getCode()));
        }
        return free.stream().map(EhrModule::getCode).toList();
    }
}
