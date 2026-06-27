package com.ehrapi.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/** A medication / prescription entry. Module code: MEDICATIONS. */
@Entity
@Table(name = "medications")
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "medications_seq")
    @SequenceGenerator(name = "medications_seq", sequenceName = "MEDICATIONS_ID_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "institution_id", nullable = false)
    private Long institutionId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 100)
    private String dosage;

    @Column(length = 100)
    private String frequency;

    @Column(length = 100)
    private String route;

    /** ACTIVE, STOPPED, COMPLETED. */
    @Column(length = 30)
    private String status = "ACTIVE";

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(length = 255)
    private String prescriber;

    public Medication() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public Long getInstitutionId() { return institutionId; }
    public void setInstitutionId(Long institutionId) { this.institutionId = institutionId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getPrescriber() { return prescriber; }
    public void setPrescriber(String prescriber) { this.prescriber = prescriber; }
}
