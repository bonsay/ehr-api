package com.ehrapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Records an institution's right to use a paid module — the commercial
 * counterpart to {@link InstitutionModule}.
 *
 * <p>The two are deliberately separate: an <em>entitlement</em> says the
 * institution is licensed (bought or trialing) a module, while
 * {@link InstitutionModule} says an admin has actually switched it on. A FREE
 * module needs no entitlement; a paid one cannot be enabled without an active
 * entitlement here. In Phase 1 entitlements are granted directly (trial /
 * local purchase); Phase 2 will create them from a billing provider webhook.
 */
@Entity
@Table(name = "module_entitlements",
        uniqueConstraints = @UniqueConstraint(columnNames = {"institution_id", "module_code"}))
public class ModuleEntitlement {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "module_entitlements_seq")
    @SequenceGenerator(name = "module_entitlements_seq", sequenceName = "MODULE_ENTITLEMENTS_ID_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "institution_id", nullable = false)
    private Long institutionId;

    @Column(name = "module_code", nullable = false, length = 50)
    private String moduleCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EntitlementStatus status;

    /** Where the entitlement came from: TRIAL, LOCAL_PURCHASE, STRIPE, ... */
    @Column(length = 50)
    private String source;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    /** When the entitlement lapses; null means it does not expire. */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public ModuleEntitlement() {
        this.startsAt = LocalDateTime.now();
    }

    public ModuleEntitlement(Long institutionId, String moduleCode) {
        this();
        this.institutionId = institutionId;
        this.moduleCode = moduleCode;
    }

    /**
     * True when this entitlement currently grants access: it is TRIAL or ACTIVE
     * and has not passed its expiry.
     */
    public boolean isActiveNow() {
        if (status != EntitlementStatus.ACTIVE && status != EntitlementStatus.TRIAL) {
            return false;
        }
        return expiresAt == null || expiresAt.isAfter(LocalDateTime.now());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getInstitutionId() { return institutionId; }
    public void setInstitutionId(Long institutionId) { this.institutionId = institutionId; }

    public String getModuleCode() { return moduleCode; }
    public void setModuleCode(String moduleCode) { this.moduleCode = moduleCode; }

    public EntitlementStatus getStatus() { return status; }
    public void setStatus(EntitlementStatus status) { this.status = status; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public LocalDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(LocalDateTime startsAt) { this.startsAt = startsAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
