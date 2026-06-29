package com.ehrapi.controller;

import com.ehrapi.common.ModuleCodes;
import com.ehrapi.entity.Problem;
import com.ehrapi.security.RequiresModule;
import com.ehrapi.service.ProblemService;
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
@Tag(name = "Problems", description = "Problem list / diagnoses (module: PROBLEMS)")
@RequiresModule(ModuleCodes.PROBLEMS)
public class ProblemController {

    private final ProblemService service;

    public ProblemController(ProblemService service) {
        this.service = service;
    }

    @GetMapping("/patients/{patientId}/problems")
    @Operation(summary = "List a patient's problems")
    public List<Problem> list(@PathVariable Long patientId) {
        return service.getForPatient(patientId);
    }

    @PostMapping("/patients/{patientId}/problems")
    @Operation(summary = "Add a problem")
    @PreAuthorize("hasAuthority('PROBLEMS:WRITE')")
    public ResponseEntity<Problem> create(@PathVariable Long patientId, @Valid @RequestBody Problem problem) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(patientId, problem));
    }

    @PutMapping("/problems/{id}")
    @Operation(summary = "Update a problem")
    @PreAuthorize("hasAuthority('PROBLEMS:WRITE')")
    public Problem update(@PathVariable Long id, @Valid @RequestBody Problem problem) {
        return service.update(id, problem);
    }

    @DeleteMapping("/problems/{id}")
    @Operation(summary = "Delete a problem")
    @PreAuthorize("hasAuthority('PROBLEMS:WRITE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
