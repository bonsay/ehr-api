package com.ehrapi.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/** A problem-list / diagnosis entry. Module code: PROBLEMS. */
@Entity
@Table(name = "problems")
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "problems_seq")
    @SequenceGenerator(name = "problems_seq", sequenceName = "PROBLEMS_ID_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "institution_id", nullable = false)
    private Long institutionId;

    /** Coded diagnosis (e.g. ICD-10). */
    @Column(length = 50)
    private String code;

    @Column(nullable = false, length = 500)
    private String description;

    /** ACTIVE or RESOLVED. */
    @Column(length = 30)
    private String status = "ACTIVE";

    @Column(name = "onset_date")
    private LocalDate onsetDate;

    @Column(name = "recorded_date")
    private LocalDate recordedDate;

    public Problem() {
        this.recordedDate = LocalDate.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public Long getInstitutionId() { return institutionId; }
    public void setInstitutionId(Long institutionId) { this.institutionId = institutionId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getOnsetDate() { return onsetDate; }
    public void setOnsetDate(LocalDate onsetDate) { this.onsetDate = onsetDate; }

    public LocalDate getRecordedDate() { return recordedDate; }
    public void setRecordedDate(LocalDate recordedDate) { this.recordedDate = recordedDate; }
}
