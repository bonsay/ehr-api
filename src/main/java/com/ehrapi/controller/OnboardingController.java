package com.ehrapi.controller;

import com.ehrapi.dto.RegisterInstitutionRequest;
import com.ehrapi.dto.RegistrationResult;
import com.ehrapi.service.OnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public self-service onboarding. Lets a prospective customer create a free-tier
 * institution and its first administrator without an existing account — the
 * frictionless adoption entry point.
 */
@RestController
@RequestMapping("/api/onboarding")
@Tag(name = "Onboarding", description = "Self-service institution sign-up (free tier)")
public class OnboardingController {

    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping("/register")
    @Operation(summary = "Create a free-tier institution and its first administrator")
    public ResponseEntity<RegistrationResult> register(@Valid @RequestBody RegisterInstitutionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(onboardingService.register(request));
    }
}
