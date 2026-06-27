package com.ehrapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** A clinical encounter / visit note. Module code: ENCOUNTERS. */
@Entity
@Table(name = "encounters")
public class Encounter {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "encounters_seq")
    @SequenceGenerator(name = "encounters_seq", sequenceName = "ENCOUNTERS_ID_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "institution_id", nullable = false)
    private Long institutionId;

    @Column(name = "encounter_date")
    private LocalDateTime encounterDate;

    @Column(length = 100)
    private String type;

    @Column(length = 500)
    private String reason;

    @Column(name = "provider_name", length = 255)
    private String providerName;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String notes;

    @Column(length = 30)
    private String status = "COMPLETED";

    public Encounter() {
        this.encounterDate = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public Long getInstitutionId() { return institutionId; }
    public void setInstitutionId(Long institutionId) { this.institutionId = institutionId; }

    public LocalDateTime getEncounterDate() { return encounterDate; }
    public void setEncounterDate(LocalDateTime encounterDate) { this.encounterDate = encounterDate; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
