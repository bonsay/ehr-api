package com.ehrapi.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/** An allergy / intolerance entry. Module code: ALLERGIES. */
@Entity
@Table(name = "allergies")
public class Allergy {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "allergies_seq")
    @SequenceGenerator(name = "allergies_seq", sequenceName = "ALLERGIES_ID_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "institution_id", nullable = false)
    private Long institutionId;

    @Column(nullable = false, length = 255)
    private String allergen;

    @Column(length = 500)
    private String reaction;

    /** MILD, MODERATE, SEVERE. */
    @Column(length = 30)
    private String severity;

    /** ACTIVE or INACTIVE. */
    @Column(length = 30)
    private String status = "ACTIVE";

    @Column(name = "recorded_date")
    private LocalDate recordedDate;

    public Allergy() {
        this.recordedDate = LocalDate.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public Long getInstitutionId() { return institutionId; }
    public void setInstitutionId(Long institutionId) { this.institutionId = institutionId; }

    public String getAllergen() { return allergen; }
    public void setAllergen(String allergen) { this.allergen = allergen; }

    public String getReaction() { return reaction; }
    public void setReaction(String reaction) { this.reaction = reaction; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getRecordedDate() { return recordedDate; }
    public void setRecordedDate(LocalDate recordedDate) { this.recordedDate = recordedDate; }
}
