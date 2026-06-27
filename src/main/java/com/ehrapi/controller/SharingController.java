package com.ehrapi.controller;

import com.ehrapi.dto.SharedPatientRecordDto;
import com.ehrapi.service.SharingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * Cross-institution sharing endpoint. Returns a patient's aggregated record to
 * a requesting institution, filtered by the patient's active consent.
 */
@RestController
@RequestMapping("/api/sharing")
@Tag(name = "Sharing", description = "Consent-gated cross-institution record sharing")
public class SharingController {

    private final SharingService sharingService;

    public SharingController(SharingService sharingService) {
        this.sharingService = sharingService;
    }

    @GetMapping("/patients/{patientId}/record")
    @Operation(summary = "Fetch a patient's shared record",
            description = "Returns the patient's record limited to the modules the patient has "
                    + "consented to share with the requesting institution. Responds 403 when no "
                    + "active consent exists.")
    public SharedPatientRecordDto getSharedRecord(
            @PathVariable Long patientId,
            @RequestParam Long requestingInstitutionId) {
        return sharingService.getSharedRecord(patientId, requestingInstitutionId);
    }
}
