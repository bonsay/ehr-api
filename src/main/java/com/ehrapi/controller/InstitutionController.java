package com.ehrapi.controller;

import com.ehrapi.entity.Institution;
import com.ehrapi.service.InstitutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/institutions")
@Tag(name = "Institutions", description = "Manage participating clinical institutions")
public class InstitutionController {

    private final InstitutionService institutionService;

    public InstitutionController(InstitutionService institutionService) {
        this.institutionService = institutionService;
    }

    @GetMapping
    @Operation(summary = "List all institutions")
    public List<Institution> getAll() {
        return institutionService.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an institution by id")
    public Institution getById(@PathVariable Long id) {
        return institutionService.getById(id);
    }

    @PostMapping
    @Operation(summary = "Register a new institution")
    public ResponseEntity<Institution> create(@Valid @RequestBody Institution institution) {
        return ResponseEntity.status(HttpStatus.CREATED).body(institutionService.create(institution));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an institution")
    public Institution update(@PathVariable Long id, @Valid @RequestBody Institution institution) {
        return institutionService.update(id, institution);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an institution")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        institutionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
