package com.ehrapi.service;

import com.ehrapi.dto.ModuleStatusDto;
import com.ehrapi.entity.EhrModule;
import com.ehrapi.entity.InstitutionModule;
import com.ehrapi.exception.ResourceNotFoundException;
import com.ehrapi.repository.EhrModuleRepository;
import com.ehrapi.repository.InstitutionModuleRepository;
import com.ehrapi.repository.InstitutionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages the module catalog and which modules each institution has enabled —
 * the heart of the "pick and choose" capability.
 */
@Service
public class ModuleService {

    private final EhrModuleRepository moduleRepository;
    private final InstitutionModuleRepository institutionModuleRepository;
    private final InstitutionRepository institutionRepository;

    public ModuleService(EhrModuleRepository moduleRepository,
                         InstitutionModuleRepository institutionModuleRepository,
                         InstitutionRepository institutionRepository) {
        this.moduleRepository = moduleRepository;
        this.institutionModuleRepository = institutionModuleRepository;
        this.institutionRepository = institutionRepository;
    }

    /** The full catalog of available modules. */
    public List<EhrModule> getCatalog() {
        return moduleRepository.findAll();
    }

    /**
     * The catalog annotated with each module's enabled/disabled state for the
     * given institution.
     */
    public List<ModuleStatusDto> getModuleStatusForInstitution(Long institutionId) {
        requireInstitution(institutionId);
        Set<String> enabled = institutionModuleRepository.findByInstitutionIdAndEnabledTrue(institutionId)
                .stream()
                .map(InstitutionModule::getModuleCode)
                .collect(Collectors.toSet());

        return moduleRepository.findAll().stream()
                .map(m -> new ModuleStatusDto(
                        m.getCode(), m.getName(), m.getDescription(), m.getCategory(),
                        m.getApiPath(), enabled.contains(m.getCode())))
                .collect(Collectors.toList());
    }

    /** The module codes an institution currently has enabled. */
    public List<String> getEnabledModuleCodes(Long institutionId) {
        return institutionModuleRepository.findByInstitutionIdAndEnabledTrue(institutionId)
                .stream()
                .map(InstitutionModule::getModuleCode)
                .collect(Collectors.toList());
    }

    /** Enable or disable a catalog module for an institution. */
    public ModuleStatusDto setModuleEnabled(Long institutionId, String moduleCode, boolean enabled) {
        requireInstitution(institutionId);
        EhrModule module = moduleRepository.findByCode(moduleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found with code: " + moduleCode));

        InstitutionModule link = institutionModuleRepository
                .findByInstitutionIdAndModuleCode(institutionId, moduleCode)
                .orElseGet(() -> new InstitutionModule(institutionId, moduleCode));
        link.setEnabled(enabled);
        institutionModuleRepository.save(link);

        return new ModuleStatusDto(module.getCode(), module.getName(), module.getDescription(),
                module.getCategory(), module.getApiPath(), enabled);
    }

    public boolean isModuleEnabled(Long institutionId, String moduleCode) {
        return institutionModuleRepository
                .findByInstitutionIdAndModuleCode(institutionId, moduleCode)
                .map(InstitutionModule::isEnabled)
                .orElse(false);
    }

    private void requireInstitution(Long institutionId) {
        if (!institutionRepository.existsById(institutionId)) {
            throw new ResourceNotFoundException("Institution not found with id: " + institutionId);
        }
    }
}
