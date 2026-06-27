package com.ehrapi.repository;

import com.ehrapi.entity.Allergy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AllergyRepository extends JpaRepository<Allergy, Long> {
    List<Allergy> findByPatientIdOrderByRecordedDateDesc(Long patientId);
    List<Allergy> findByPatientIdAndInstitutionId(Long patientId, Long institutionId);
}
