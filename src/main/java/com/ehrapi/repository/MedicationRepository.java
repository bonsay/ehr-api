package com.ehrapi.repository;

import com.ehrapi.entity.Medication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicationRepository extends JpaRepository<Medication, Long> {
    List<Medication> findByPatientIdOrderByStartDateDesc(Long patientId);
    List<Medication> findByPatientIdAndInstitutionId(Long patientId, Long institutionId);
}
