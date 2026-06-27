package com.ehrapi.repository;

import com.ehrapi.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long> {
    List<Problem> findByPatientIdOrderByRecordedDateDesc(Long patientId);
    List<Problem> findByPatientIdAndInstitutionId(Long patientId, Long institutionId);
}
