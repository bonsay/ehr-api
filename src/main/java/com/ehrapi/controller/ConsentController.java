package com.ehrapi.controller;

import com.ehrapi.dto.GrantConsentRequest;
import com.ehrapi.entity.PatientConsent;
import com.ehrapi.service.ConsentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Patient consent management. Consents authorise other institutions to access
 * the patient's record for cross-institution sharing.
 */
@RestController
@RequestMapping("/api/patients/{patientId}/consents")
@Tag(name = "Consents", description = "Patient-controlled sharing permissions")
public class ConsentController {

    private final ConsentService consentService;

    public ConsentController(ConsentService consentService) {
        this.consentService = consentService;
    }

    @GetMapping
    @Operation(summary = "List a patient's sharing consents")
    public List<PatientConsent> list(@PathVariable Long patientId) {
        return consentService.getConsentsForPatient(patientId);
    }

    @PostMapping
    @Operation(summary = "Grant consent to share the patient's record with an institution")
    public ResponseEntity<PatientConsent> grant(@PathVariable Long patientId,
                                                 @Valid @RequestBody GrantConsentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(consentService.grant(patientId, request));
    }

    @PostMapping("/{consentId}/revoke")
    @Operation(summary = "Revoke a previously granted consent")
    public PatientConsent revoke(@PathVariable Long patientId, @PathVariable Long consentId) {
        return consentService.revoke(consentId);
    }
}
