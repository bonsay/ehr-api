package com.ehrapi.repository;

import com.ehrapi.entity.EhrModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EhrModuleRepository extends JpaRepository<EhrModule, Long> {
    Optional<EhrModule> findByCode(String code);
}
