package com.ehrapi.repository;

import com.ehrapi.entity.PatientConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientConsentRepository extends JpaRepository<PatientConsent, Long> {
    List<PatientConsent> findByPatientId(Long patientId);
    List<PatientConsent> findByPatientIdAndStatus(Long patientId, String status);
    List<PatientConsent> findByPatientIdAndGrantedToInstitutionIdAndStatus(
            Long patientId, Long grantedToInstitutionId, String status);
}
