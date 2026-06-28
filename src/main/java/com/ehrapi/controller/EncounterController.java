package com.ehrapi.controller;

import com.ehrapi.entity.Encounter;
import com.ehrapi.service.EncounterService;
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
@Tag(name = "Encounters", description = "Clinical encounters / visit notes (module: ENCOUNTERS)")
public class EncounterController {

    private final EncounterService service;

    public EncounterController(EncounterService service) {
        this.service = service;
    }

    @GetMapping("/patients/{patientId}/encounters")
    @Operation(summary = "List a patient's encounters")
    public List<Encounter> list(@PathVariable Long patientId) {
        return service.getForPatient(patientId);
    }

    @PostMapping("/patients/{patientId}/encounters")
    @Operation(summary = "Record a new encounter")
    @PreAuthorize("hasAuthority('ENCOUNTERS:WRITE')")
    public ResponseEntity<Encounter> create(@PathVariable Long patientId, @Valid @RequestBody Encounter encounter) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(patientId, encounter));
    }

    @PutMapping("/encounters/{id}")
    @Operation(summary = "Update an encounter")
    @PreAuthorize("hasAuthority('ENCOUNTERS:WRITE')")
    public Encounter update(@PathVariable Long id, @Valid @RequestBody Encounter encounter) {
        return service.update(id, encounter);
    }

    @DeleteMapping("/encounters/{id}")
    @Operation(summary = "Delete an encounter")
    @PreAuthorize("hasAuthority('ENCOUNTERS:WRITE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
