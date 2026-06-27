package com.ehrapi.repository;

import com.ehrapi.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByMrn(String mrn);

    List<Patient> findByHomeInstitutionId(Long homeInstitutionId);

    @Query("SELECT p FROM Patient p WHERE " +
            "LOWER(p.firstName) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
            "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
            "LOWER(p.mrn) LIKE LOWER(CONCAT('%', :term, '%'))")
    List<Patient> search(@Param("term") String term);
}
