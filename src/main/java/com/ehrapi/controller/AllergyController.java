package com.ehrapi.controller;

import com.ehrapi.entity.Allergy;
import com.ehrapi.service.AllergyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Allergies", description = "Allergies / intolerances (module: ALLERGIES)")
public class AllergyController {

    private final AllergyService service;

    public AllergyController(AllergyService service) {
        this.service = service;
    }

    @GetMapping("/patients/{patientId}/allergies")
    @Operation(summary = "List a patient's allergies")
    public List<Allergy> list(@PathVariable Long patientId) {
        return service.getForPatient(patientId);
    }

    @PostMapping("/patients/{patientId}/allergies")
    @Operation(summary = "Add an allergy")
    public ResponseEntity<Allergy> create(@PathVariable Long patientId, @Valid @RequestBody Allergy allergy) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(patientId, allergy));
    }

    @PutMapping("/allergies/{id}")
    @Operation(summary = "Update an allergy")
    public Allergy update(@PathVariable Long id, @Valid @RequestBody Allergy allergy) {
        return service.update(id, allergy);
    }

    @DeleteMapping("/allergies/{id}")
    @Operation(summary = "Delete an allergy")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
