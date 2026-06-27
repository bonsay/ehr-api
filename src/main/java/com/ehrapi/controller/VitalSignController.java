package com.ehrapi.controller;

import com.ehrapi.entity.VitalSign;
import com.ehrapi.service.VitalSignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Vitals", description = "Vital signs (module: VITALS)")
public class VitalSignController {

    private final VitalSignService service;

    public VitalSignController(VitalSignService service) {
        this.service = service;
    }

    @GetMapping("/patients/{patientId}/vitals")
    @Operation(summary = "List a patient's vital signs")
    public List<VitalSign> list(@PathVariable Long patientId) {
        return service.getForPatient(patientId);
    }

    @PostMapping("/patients/{patientId}/vitals")
    @Operation(summary = "Record vital signs")
    public ResponseEntity<VitalSign> create(@PathVariable Long patientId, @Valid @RequestBody VitalSign vital) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(patientId, vital));
    }

    @PutMapping("/vitals/{id}")
    @Operation(summary = "Update vital signs")
    public VitalSign update(@PathVariable Long id, @Valid @RequestBody VitalSign vital) {
        return service.update(id, vital);
    }

    @DeleteMapping("/vitals/{id}")
    @Operation(summary = "Delete vital signs")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
