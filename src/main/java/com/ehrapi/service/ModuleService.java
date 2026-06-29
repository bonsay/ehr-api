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
    private final EntitlementService entitlementService;

    public ModuleService(EhrModuleRepository moduleRepository,
                         InstitutionModuleRepository institutionModuleRepository,
                         InstitutionRepository institutionRepository,
                         EntitlementService entitlementService) {
        this.moduleRepository = moduleRepository;
        this.institutionModuleRepository = institutionModuleRepository;
        this.institutionRepository = institutionRepository;
        this.entitlementService = entitlementService;
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
                .map(m -> toStatus(m, institutionId, enabled.contains(m.getCode())))
                .collect(Collectors.toList());
    }

    /** The status of a single module for an institution (used after a mutation). */
    public ModuleStatusDto getStatus(Long institutionId, String moduleCode) {
        requireInstitution(institutionId);
        EhrModule module = requireModule(moduleCode);
        return toStatus(module, institutionId, isModuleEnabled(institutionId, moduleCode));
    }

    /** Build the marketplace DTO, folding in the institution's entitlement state. */
    private ModuleStatusDto toStatus(EhrModule m, Long institutionId, boolean enabled) {
        return new ModuleStatusDto(
                m.getCode(), m.getName(), m.getDescription(), m.getCategory(), m.getApiPath(),
                enabled, m.getTier(), m.getPriceModel(), m.getPriceMonthlyCents(),
                entitlementService.isEntitled(institutionId, m.getCode()),
                entitlementService.statusFor(institutionId, m.getCode()));
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
        EhrModule module = requireModule(moduleCode);

        // A paid module can only be switched on with an active entitlement; this
        // is the commercial gate at the marketplace toggle (mirrored server-side
        // on the module's endpoints by ModuleEntitlementInterceptor).
        if (enabled) {
            entitlementService.requireEntitled(institutionId, moduleCode);
        }

        InstitutionModule link = institutionModuleRepository
                .findByInstitutionIdAndModuleCode(institutionId, moduleCode)
                .orElseGet(() -> new InstitutionModule(institutionId, moduleCode));
        link.setEnabled(enabled);
        institutionModuleRepository.save(link);

        return toStatus(module, institutionId, enabled);
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

    private EhrModule requireModule(String moduleCode) {
        return moduleRepository.findByCode(moduleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found with code: " + moduleCode));
    }
}
