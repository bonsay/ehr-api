package com.ehrapi.service;

import com.ehrapi.entity.EhrModule;
import com.ehrapi.entity.EntitlementStatus;
import com.ehrapi.entity.ModuleEntitlement;
import com.ehrapi.exception.ModuleNotEntitledException;
import com.ehrapi.exception.ResourceNotFoundException;
import com.ehrapi.repository.EhrModuleRepository;
import com.ehrapi.repository.ModuleEntitlementRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Owns the commercial "right to use" a module — the monetization layer that sits
 * on top of the module catalog. FREE modules are entitled to everyone; paid
 * (PRO/ENTERPRISE) modules require an active {@link ModuleEntitlement}.
 *
 * <p>Phase 1 grants entitlements directly via {@link #startTrial} and
 * {@link #purchase} (a self-contained local grant, mirroring the platform's
 * local auth mode). Phase 2 will create/refresh entitlements from a billing
 * provider (e.g. Stripe) webhook instead, leaving the rest of this API intact.
 */
@Service
public class EntitlementService {

    /** Default evaluation length for a self-service trial. */
    public static final int DEFAULT_TRIAL_DAYS = 30;

    private final ModuleEntitlementRepository entitlements;
    private final EhrModuleRepository modules;

    public EntitlementService(ModuleEntitlementRepository entitlements, EhrModuleRepository modules) {
        this.entitlements = entitlements;
        this.modules = modules;
    }

    /** Whether the institution may currently use the given module. */
    public boolean isEntitled(Long institutionId, String moduleCode) {
        EhrModule module = modules.findByCode(moduleCode).orElse(null);
        // Unknown or free modules carry no commercial restriction.
        if (module == null || !module.isPaid()) {
            return true;
        }
        return activeEntitlement(institutionId, moduleCode).isPresent();
    }

    /**
     * The status to surface for a module: the stored status when an entitlement
     * exists, otherwise null (never licensed). Independent of expiry so the UI
     * can show e.g. EXPIRED.
     */
    public EntitlementStatus statusFor(Long institutionId, String moduleCode) {
        return entitlements.findByInstitutionIdAndModuleCode(institutionId, moduleCode)
                .map(ModuleEntitlement::getStatus)
                .orElse(null);
    }

    /** Throws {@link ModuleNotEntitledException} unless the institution is entitled. */
    public void requireEntitled(Long institutionId, String moduleCode) {
        if (!isEntitled(institutionId, moduleCode)) {
            throw new ModuleNotEntitledException(
                    "Module '" + moduleCode + "' requires a subscription. Start a trial or purchase it to continue.");
        }
    }

    public List<ModuleEntitlement> listForInstitution(Long institutionId) {
        return entitlements.findByInstitutionId(institutionId);
    }

    /** Start (or restart) a time-limited trial for a paid module. */
    public ModuleEntitlement startTrial(Long institutionId, String moduleCode) {
        requirePaidModule(moduleCode);
        return grant(institutionId, moduleCode, EntitlementStatus.TRIAL, "TRIAL",
                LocalDateTime.now().plusDays(DEFAULT_TRIAL_DAYS));
    }

    /**
     * Purchase a paid module. In Phase 1 this is a direct local grant (no payment
     * gateway); Phase 2 replaces the body with a checkout + webhook flow.
     */
    public ModuleEntitlement purchase(Long institutionId, String moduleCode) {
        requirePaidModule(moduleCode);
        return grant(institutionId, moduleCode, EntitlementStatus.ACTIVE, "LOCAL_PURCHASE", null);
    }

    /**
     * Activate (or renew) an entitlement from a settled billing event. Used by
     * asynchronous providers once payment is confirmed via webhook.
     */
    public ModuleEntitlement activate(Long institutionId, String moduleCode, String source,
                                      LocalDateTime expiresAt) {
        return grant(institutionId, moduleCode, EntitlementStatus.ACTIVE, source, expiresAt);
    }

    /** Mark an existing entitlement expired (e.g. subscription cancelled/lapsed). */
    public void expire(Long institutionId, String moduleCode) {
        entitlements.findByInstitutionIdAndModuleCode(institutionId, moduleCode).ifPresent(e -> {
            e.setStatus(EntitlementStatus.EXPIRED);
            entitlements.save(e);
        });
    }

    /** Upsert the institution's entitlement for a module. */
    public ModuleEntitlement grant(Long institutionId, String moduleCode, EntitlementStatus status,
                                   String source, LocalDateTime expiresAt) {
        ModuleEntitlement entitlement = entitlements
                .findByInstitutionIdAndModuleCode(institutionId, moduleCode)
                .orElseGet(() -> new ModuleEntitlement(institutionId, moduleCode));
        entitlement.setStatus(status);
        entitlement.setSource(source);
        entitlement.setStartsAt(LocalDateTime.now());
        entitlement.setExpiresAt(expiresAt);
        return entitlements.save(entitlement);
    }

    private Optional<ModuleEntitlement> activeEntitlement(Long institutionId, String moduleCode) {
        return entitlements.findByInstitutionIdAndModuleCode(institutionId, moduleCode)
                .filter(ModuleEntitlement::isActiveNow);
    }

    private void requirePaidModule(String moduleCode) {
        EhrModule module = modules.findByCode(moduleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found with code: " + moduleCode));
        if (!module.isPaid()) {
            throw new IllegalArgumentException("Module '" + moduleCode + "' is free and needs no entitlement.");
        }
    }
}
