package com.ehrapi.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/** Request body for a patient granting consent to share their record. */
public class GrantConsentRequest {

    @NotNull(message = "grantedToInstitutionId is required")
    private Long grantedToInstitutionId;

    /** "ALL" or comma-separated module codes. Defaults to ALL when omitted. */
    private String scope = "ALL";

    private LocalDateTime expiryDate;

    public Long getGrantedToInstitutionId() { return grantedToInstitutionId; }
    public void setGrantedToInstitutionId(Long grantedToInstitutionId) { this.grantedToInstitutionId = grantedToInstitutionId; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
}
