package com.ehrapi.repository;

import com.ehrapi.entity.VitalSign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VitalSignRepository extends JpaRepository<VitalSign, Long> {
    List<VitalSign> findByPatientIdOrderByRecordedDateDesc(Long patientId);
    List<VitalSign> findByPatientIdAndInstitutionId(Long patientId, Long institutionId);
}
