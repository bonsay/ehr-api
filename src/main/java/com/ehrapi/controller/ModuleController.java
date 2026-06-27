package com.ehrapi.controller;

import com.ehrapi.dto.ModuleStatusDto;
import com.ehrapi.dto.ToggleModuleRequest;
import com.ehrapi.entity.EhrModule;
import com.ehrapi.service.ModuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Module catalog and per-institution enablement — the "pick and choose"
 * surface of the platform.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Modules", description = "EHR module catalog and per-institution enablement")
public class ModuleController {

    private final ModuleService moduleService;

    public ModuleController(ModuleService moduleService) {
        this.moduleService = moduleService;
    }

    @GetMapping("/modules")
    @Operation(summary = "Get the full catalog of available EHR modules")
    public List<EhrModule> getCatalog() {
        return moduleService.getCatalog();
    }

    @GetMapping("/institutions/{institutionId}/modules")
    @Operation(summary = "Get module catalog with enabled/disabled state for an institution")
    public List<ModuleStatusDto> getForInstitution(@PathVariable Long institutionId) {
        return moduleService.getModuleStatusForInstitution(institutionId);
    }

    @PutMapping("/institutions/{institutionId}/modules/{moduleCode}")
    @Operation(summary = "Enable or disable a module for an institution")
    public ModuleStatusDto toggle(@PathVariable Long institutionId,
                                  @PathVariable String moduleCode,
                                  @RequestBody ToggleModuleRequest request) {
        return moduleService.setModuleEnabled(institutionId, moduleCode, request.isEnabled());
    }
}
