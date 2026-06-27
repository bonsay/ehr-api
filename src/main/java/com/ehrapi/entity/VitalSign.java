package com.ehrapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** A set of recorded vital signs. Module code: VITALS. */
@Entity
@Table(name = "vital_signs")
public class VitalSign {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vitals_seq")
    @SequenceGenerator(name = "vitals_seq", sequenceName = "VITALS_ID_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "institution_id", nullable = false)
    private Long institutionId;

    @Column(name = "recorded_date")
    private LocalDateTime recordedDate;

    @Column(name = "blood_pressure", length = 20)
    private String bloodPressure;

    @Column(name = "heart_rate")
    private Integer heartRate;

    @Column(name = "respiratory_rate")
    private Integer respiratoryRate;

    private Double temperature;

    @Column(name = "oxygen_saturation")
    private Integer oxygenSaturation;

    /** Height in centimetres. */
    private Double height;

    /** Weight in kilograms. */
    private Double weight;

    public VitalSign() {
        this.recordedDate = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public Long getInstitutionId() { return institutionId; }
    public void setInstitutionId(Long institutionId) { this.institutionId = institutionId; }

    public LocalDateTime getRecordedDate() { return recordedDate; }
    public void setRecordedDate(LocalDateTime recordedDate) { this.recordedDate = recordedDate; }

    public String getBloodPressure() { return bloodPressure; }
    public void setBloodPressure(String bloodPressure) { this.bloodPressure = bloodPressure; }

    public Integer getHeartRate() { return heartRate; }
    public void setHeartRate(Integer heartRate) { this.heartRate = heartRate; }

    public Integer getRespiratoryRate() { return respiratoryRate; }
    public void setRespiratoryRate(Integer respiratoryRate) { this.respiratoryRate = respiratoryRate; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public Integer getOxygenSaturation() { return oxygenSaturation; }
    public void setOxygenSaturation(Integer oxygenSaturation) { this.oxygenSaturation = oxygenSaturation; }

    public Double getHeight() { return height; }
    public void setHeight(Double height) { this.height = height; }

    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
}
