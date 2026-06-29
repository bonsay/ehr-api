package com.ehrapi.dto;

import com.ehrapi.entity.EntitlementStatus;
import com.ehrapi.entity.ModuleEntitlement;

import java.time.LocalDateTime;

/** An institution's entitlement to a single module, for the admin/billing view. */
public class EntitlementDto {

    private String moduleCode;
    private EntitlementStatus status;
    private String source;
    private LocalDateTime startsAt;
    private LocalDateTime expiresAt;
    private boolean active;

    public EntitlementDto() {}

    public static EntitlementDto from(ModuleEntitlement e) {
        EntitlementDto dto = new EntitlementDto();
        dto.moduleCode = e.getModuleCode();
        dto.status = e.getStatus();
        dto.source = e.getSource();
        dto.startsAt = e.getStartsAt();
        dto.expiresAt = e.getExpiresAt();
        dto.active = e.isActiveNow();
        return dto;
    }

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

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
