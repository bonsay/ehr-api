package com.ehrapi.service;

import com.ehrapi.dto.GrantConsentRequest;
import com.ehrapi.entity.PatientConsent;
import com.ehrapi.exception.ResourceNotFoundException;
import com.ehrapi.repository.InstitutionRepository;
import com.ehrapi.repository.PatientConsentRepository;
import com.ehrapi.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages patient consents that authorise cross-institution sharing, and
 * decides whether a requesting institution may see a given module.
 */
@Service
public class ConsentService {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_REVOKED = "REVOKED";
    public static final String SCOPE_ALL = "ALL";

    private final PatientConsentRepository consentRepository;
    private final PatientRepository patientRepository;
    private final InstitutionRepository institutionRepository;

    public ConsentService(PatientConsentRepository consentRepository,
                          PatientRepository patientRepository,
                          InstitutionRepository institutionRepository) {
        this.consentRepository = consentRepository;
        this.patientRepository = patientRepository;
        this.institutionRepository = institutionRepository;
    }

    public List<PatientConsent> getConsentsForPatient(Long patientId) {
        requirePatient(patientId);
        return consentRepository.findByPatientId(patientId);
    }

    /** Patient grants an institution access to all or part of their record. */
    public PatientConsent grant(Long patientId, GrantConsentRequest request) {
        requirePatient(patientId);
        if (request.getGrantedToInstitutionId() == null
                || !institutionRepository.existsById(request.getGrantedToInstitutionId())) {
            throw new ResourceNotFoundException(
                    "Institution not found with id: " + request.getGrantedToInstitutionId());
        }

        PatientConsent consent = new PatientConsent();
        consent.setPatientId(patientId);
        consent.setGrantedToInstitutionId(request.getGrantedToInstitutionId());
        consent.setScope(request.getScope() == null || request.getScope().isBlank()
                ? SCOPE_ALL : request.getScope().trim());
        consent.setStatus(STATUS_ACTIVE);
        consent.setGrantedDate(LocalDateTime.now());
        consent.setExpiryDate(request.getExpiryDate());
        return consentRepository.save(consent);
    }

    public PatientConsent revoke(Long consentId) {
        PatientConsent consent = consentRepository.findById(consentId)
                .orElseThrow(() -> new ResourceNotFoundException("Consent not found with id: " + consentId));
        consent.setStatus(STATUS_REVOKED);
        consent.setRevokedDate(LocalDateTime.now());
        return consentRepository.save(consent);
    }

    /**
     * The set of module codes a requesting institution is permitted to view for
     * a patient, based on active, non-expired consents. An institution always
     * has full access to a patient registered at that same institution.
     */
    public Set<String> permittedModules(Long patientId, Long requestingInstitutionId, List<String> allModuleCodes) {
        return patientRepository.findById(patientId)
                .map(p -> resolvePermitted(p, requestingInstitutionId, allModuleCodes))
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientId));
    }

    private Set<String> resolvePermitted(com.ehrapi.entity.Patient patient,
                                         Long requestingInstitutionId,
                                         List<String> allModuleCodes) {
        // Home institution sees everything.
        if (requestingInstitutionId != null
                && requestingInstitutionId.equals(patient.getHomeInstitutionId())) {
            return new java.util.HashSet<>(allModuleCodes);
        }

        LocalDateTime now = LocalDateTime.now();
        Set<String> permitted = consentRepository
                .findByPatientIdAndGrantedToInstitutionIdAndStatus(
                        patient.getId(), requestingInstitutionId, STATUS_ACTIVE)
                .stream()
                .filter(c -> c.getExpiryDate() == null || c.getExpiryDate().isAfter(now))
                .flatMap(c -> expandScope(c.getScope(), allModuleCodes).stream())
                .collect(Collectors.toSet());
        return permitted;
    }

    private Set<String> expandScope(String scope, List<String> allModuleCodes) {
        if (scope == null || scope.isBlank() || SCOPE_ALL.equalsIgnoreCase(scope.trim())) {
            return new java.util.HashSet<>(allModuleCodes);
        }
        return Arrays.stream(scope.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private void requirePatient(Long patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient not found with id: " + patientId);
        }
    }
}
