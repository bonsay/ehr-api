package com.ehrapi.service;

import com.ehrapi.common.ModuleCodes;
import com.ehrapi.dto.SharedPatientRecordDto;
import com.ehrapi.entity.Patient;
import com.ehrapi.exception.ConsentDeniedException;
import com.ehrapi.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Assembles a patient's record for a requesting institution, enforcing the
 * patient's consent. This is what allows multiple clinical institutions to
 * share patient information and history — but only with the patient's
 * permission, and only for the modules the patient agreed to.
 */
@Service
public class SharingService {

    private static final List<String> CLINICAL_MODULES = List.of(
            ModuleCodes.ENCOUNTERS, ModuleCodes.PROBLEMS, ModuleCodes.MEDICATIONS,
            ModuleCodes.ALLERGIES, ModuleCodes.VITALS);

    private final ConsentService consentService;
    private final PatientRepository patientRepository;
    private final EncounterRepository encounterRepository;
    private final ProblemRepository problemRepository;
    private final MedicationRepository medicationRepository;
    private final AllergyRepository allergyRepository;
    private final VitalSignRepository vitalSignRepository;

    public SharingService(ConsentService consentService,
                          PatientRepository patientRepository,
                          EncounterRepository encounterRepository,
                          ProblemRepository problemRepository,
                          MedicationRepository medicationRepository,
                          AllergyRepository allergyRepository,
                          VitalSignRepository vitalSignRepository) {
        this.consentService = consentService;
        this.patientRepository = patientRepository;
        this.encounterRepository = encounterRepository;
        this.problemRepository = problemRepository;
        this.medicationRepository = medicationRepository;
        this.allergyRepository = allergyRepository;
        this.vitalSignRepository = vitalSignRepository;
    }

    /**
     * Build the consent-filtered, cross-institution record for a patient.
     *
     * @param patientId               the patient whose record is requested
     * @param requestingInstitutionId the institution asking for the record
     */
    public SharedPatientRecordDto getSharedRecord(Long patientId, Long requestingInstitutionId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new com.ehrapi.exception.ResourceNotFoundException(
                        "Patient not found with id: " + patientId));

        Set<String> permitted = consentService.permittedModules(
                patientId, requestingInstitutionId, CLINICAL_MODULES);

        if (permitted.isEmpty()) {
            throw new ConsentDeniedException(
                    "Institution " + requestingInstitutionId + " has no active consent to access patient "
                            + patientId + "'s record.");
        }

        SharedPatientRecordDto record = new SharedPatientRecordDto();
        record.setRequestingInstitutionId(requestingInstitutionId);
        // Demographics travel with any granted clinical module so the receiving
        // clinician can identify the patient.
        record.setPatient(patient);

        for (String module : CLINICAL_MODULES) {
            if (permitted.contains(module)) {
                record.getSharedModules().add(module);
                populateModule(record, module, patientId);
            } else {
                record.getDeniedModules().add(module);
            }
        }
        return record;
    }

    private void populateModule(SharedPatientRecordDto record, String module, Long patientId) {
        switch (module) {
            case ModuleCodes.ENCOUNTERS ->
                    record.setEncounters(encounterRepository.findByPatientIdOrderByEncounterDateDesc(patientId));
            case ModuleCodes.PROBLEMS ->
                    record.setProblems(problemRepository.findByPatientIdOrderByRecordedDateDesc(patientId));
            case ModuleCodes.MEDICATIONS ->
                    record.setMedications(medicationRepository.findByPatientIdOrderByStartDateDesc(patientId));
            case ModuleCodes.ALLERGIES ->
                    record.setAllergies(allergyRepository.findByPatientIdOrderByRecordedDateDesc(patientId));
            case ModuleCodes.VITALS ->
                    record.setVitalSigns(vitalSignRepository.findByPatientIdOrderByRecordedDateDesc(patientId));
            default -> { /* no-op */ }
        }
    }
}
