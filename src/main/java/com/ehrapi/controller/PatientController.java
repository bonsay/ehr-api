package com.ehrapi.controller;

import com.ehrapi.common.ModuleCodes;
import com.ehrapi.entity.Patient;
import com.ehrapi.security.RequiresModule;
import com.ehrapi.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
@Tag(name = "Patients", description = "Patient demographics and registration")
@RequiresModule(ModuleCodes.DEMOGRAPHICS)
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    @Operation(summary = "List or search patients")
    public List<Patient> list(@RequestParam(required = false) String search,
                              @RequestParam(required = false) Long institutionId) {
        if (institutionId != null) {
            return patientService.getByInstitution(institutionId);
        }
        if (search != null) {
            return patientService.search(search);
        }
        return patientService.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a patient by id")
    public Patient getById(@PathVariable Long id) {
        return patientService.getById(id);
    }

    @PostMapping
    @Operation(summary = "Register a new patient")
    @PreAuthorize("hasAuthority('DEMOGRAPHICS:WRITE')")
    public ResponseEntity<Patient> create(@Valid @RequestBody Patient patient) {
        return ResponseEntity.status(HttpStatus.CREATED).body(patientService.create(patient));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a patient")
    @PreAuthorize("hasAuthority('DEMOGRAPHICS:WRITE')")
    public Patient update(@PathVariable Long id, @Valid @RequestBody Patient patient) {
        return patientService.update(id, patient);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a patient")
    @PreAuthorize("hasAuthority('DEMOGRAPHICS:WRITE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        patientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
