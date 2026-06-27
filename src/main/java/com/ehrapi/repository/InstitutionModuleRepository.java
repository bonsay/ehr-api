package com.ehrapi.repository;

import com.ehrapi.entity.InstitutionModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstitutionModuleRepository extends JpaRepository<InstitutionModule, Long> {
    List<InstitutionModule> findByInstitutionId(Long institutionId);
    List<InstitutionModule> findByInstitutionIdAndEnabledTrue(Long institutionId);
    Optional<InstitutionModule> findByInstitutionIdAndModuleCode(Long institutionId, String moduleCode);
}
