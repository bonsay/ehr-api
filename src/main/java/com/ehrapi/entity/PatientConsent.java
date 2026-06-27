package com.ehrapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A patient's permission for another institution to access their record.
 *
 * <p>Consent is the gate for cross-institution sharing: clinical data is only
 * exposed to a requesting institution when an ACTIVE, non-expired consent grants
 * it. The {@code scope} is either "ALL" or a comma-separated list of module
 * codes the patient agreed to share.
 */
@Entity
@Table(name = "patient_consents")
public class PatientConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "consents_seq")
    @SequenceGenerator(name = "consents_seq", sequenceName = "CONSENTS_ID_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    /** Institution the patient is granting access to. */
    @Column(name = "granted_to_institution_id", nullable = false)
    private Long grantedToInstitutionId;

    /** "ALL" or comma-separated module codes (e.g. "PROBLEMS,MEDICATIONS"). */
    @Column(nullable = false, length = 1000)
    private String scope = "ALL";

    /** ACTIVE or REVOKED. */
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "granted_date")
    private LocalDateTime grantedDate;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "revoked_date")
    private LocalDateTime revokedDate;

    public PatientConsent() {
        this.grantedDate = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public Long getGrantedToInstitutionId() { return grantedToInstitutionId; }
    public void setGrantedToInstitutionId(Long grantedToInstitutionId) { this.grantedToInstitutionId = grantedToInstitutionId; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getGrantedDate() { return grantedDate; }
    public void setGrantedDate(LocalDateTime grantedDate) { this.grantedDate = grantedDate; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public LocalDateTime getRevokedDate() { return revokedDate; }
    public void setRevokedDate(LocalDateTime revokedDate) { this.revokedDate = revokedDate; }
}
