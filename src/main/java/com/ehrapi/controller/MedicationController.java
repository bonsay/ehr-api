package com.ehrapi.controller;

import com.ehrapi.entity.Medication;
import com.ehrapi.service.MedicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Medications", description = "Medications / prescriptions (module: MEDICATIONS)")
public class MedicationController {

    private final MedicationService service;

    public MedicationController(MedicationService service) {
        this.service = service;
    }

    @GetMapping("/patients/{patientId}/medications")
    @Operation(summary = "List a patient's medications")
    public List<Medication> list(@PathVariable Long patientId) {
        return service.getForPatient(patientId);
    }

    @PostMapping("/patients/{patientId}/medications")
    @Operation(summary = "Add a medication")
    @PreAuthorize("hasAuthority('MEDICATIONS:WRITE')")
    public ResponseEntity<Medication> create(@PathVariable Long patientId, @Valid @RequestBody Medication medication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(patientId, medication));
    }

    @PutMapping("/medications/{id}")
    @Operation(summary = "Update a medication")
    @PreAuthorize("hasAuthority('MEDICATIONS:WRITE')")
    public Medication update(@PathVariable Long id, @Valid @RequestBody Medication medication) {
        return service.update(id, medication);
    }

    @DeleteMapping("/medications/{id}")
    @Operation(summary = "Delete a medication")
    @PreAuthorize("hasAuthority('MEDICATIONS:WRITE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
