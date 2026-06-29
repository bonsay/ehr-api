package com.ehrapi.repository;

import com.ehrapi.entity.ModuleEntitlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModuleEntitlementRepository extends JpaRepository<ModuleEntitlement, Long> {
    List<ModuleEntitlement> findByInstitutionId(Long institutionId);
    Optional<ModuleEntitlement> findByInstitutionIdAndModuleCode(Long institutionId, String moduleCode);
}
