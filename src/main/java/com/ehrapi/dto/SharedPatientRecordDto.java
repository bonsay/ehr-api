package com.ehrapi.dto;

import com.ehrapi.entity.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregated, consent-filtered view of a patient's record returned to a
 * requesting institution. Only the modules permitted by the patient's consent
 * are populated; the rest are listed in {@code deniedModules}.
 */
public class SharedPatientRecordDto {

    private Patient patient;
    private Long requestingInstitutionId;
    private List<String> sharedModules = new ArrayList<>();
    private List<String> deniedModules = new ArrayList<>();

    private List<Encounter> encounters = new ArrayList<>();
    private List<Problem> problems = new ArrayList<>();
    private List<Medication> medications = new ArrayList<>();
    private List<Allergy> allergies = new ArrayList<>();
    private List<VitalSign> vitalSigns = new ArrayList<>();

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public Long getRequestingInstitutionId() { return requestingInstitutionId; }
    public void setRequestingInstitutionId(Long requestingInstitutionId) { this.requestingInstitutionId = requestingInstitutionId; }

    public List<String> getSharedModules() { return sharedModules; }
    public void setSharedModules(List<String> sharedModules) { this.sharedModules = sharedModules; }

    public List<String> getDeniedModules() { return deniedModules; }
    public void setDeniedModules(List<String> deniedModules) { this.deniedModules = deniedModules; }

    public List<Encounter> getEncounters() { return encounters; }
    public void setEncounters(List<Encounter> encounters) { this.encounters = encounters; }

    public List<Problem> getProblems() { return problems; }
    public void setProblems(List<Problem> problems) { this.problems = problems; }

    public List<Medication> getMedications() { return medications; }
    public void setMedications(List<Medication> medications) { this.medications = medications; }

    public List<Allergy> getAllergies() { return allergies; }
    public void setAllergies(List<Allergy> allergies) { this.allergies = allergies; }

    public List<VitalSign> getVitalSigns() { return vitalSigns; }
    public void setVitalSigns(List<VitalSign> vitalSigns) { this.vitalSigns = vitalSigns; }
}
