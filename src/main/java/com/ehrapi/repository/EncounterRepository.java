package com.ehrapi.repository;

import com.ehrapi.entity.Encounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EncounterRepository extends JpaRepository<Encounter, Long> {
    List<Encounter> findByPatientIdOrderByEncounterDateDesc(Long patientId);
    List<Encounter> findByPatientIdAndInstitutionId(Long patientId, Long institutionId);
}
