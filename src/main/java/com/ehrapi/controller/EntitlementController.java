package com.ehrapi.controller;

import com.ehrapi.billing.BillingProvider;
import com.ehrapi.billing.CheckoutResult;
import com.ehrapi.dto.EntitlementDto;
import com.ehrapi.dto.ModuleStatusDto;
import com.ehrapi.service.EntitlementService;
import com.ehrapi.service.ModuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Commercial surface of the marketplace: an institution's module entitlements
 * and the actions that create them (trial / purchase).
 *
 * <p>Granting is gated by {@code ADMIN:MODULES}, the same permission that governs
 * enabling modules. In Phase 1 a purchase is a direct local grant; Phase 2 will
 * route it through a billing provider checkout + webhook without changing this API.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Entitlements", description = "Module subscriptions / purchases (marketplace billing)")
public class EntitlementController {

    private final EntitlementService entitlementService;
    private final ModuleService moduleService;
    private final BillingProvider billingProvider;

    public EntitlementController(EntitlementService entitlementService, ModuleService moduleService,
                                 BillingProvider billingProvider) {
        this.entitlementService = entitlementService;
        this.moduleService = moduleService;
        this.billingProvider = billingProvider;
    }

    @GetMapping("/institutions/{institutionId}/entitlements")
    @Operation(summary = "List an institution's module entitlements")
    public List<EntitlementDto> list(@PathVariable Long institutionId) {
        return entitlementService.listForInstitution(institutionId).stream()
                .map(EntitlementDto::from)
                .toList();
    }

    @PostMapping("/institutions/{institutionId}/modules/{moduleCode}/trial")
    @Operation(summary = "Start a free trial of a paid module")
    @PreAuthorize("hasAuthority('ADMIN:MODULES')")
    public ModuleStatusDto startTrial(@PathVariable Long institutionId, @PathVariable String moduleCode) {
        entitlementService.startTrial(institutionId, moduleCode);
        return moduleService.getStatus(institutionId, moduleCode);
    }

    @PostMapping("/institutions/{institutionId}/modules/{moduleCode}/purchase")
    @Operation(summary = "Purchase a paid module via the active billing provider")
    @PreAuthorize("hasAuthority('ADMIN:MODULES')")
    public CheckoutResult purchase(@PathVariable Long institutionId, @PathVariable String moduleCode) {
        // The provider either grants immediately (local) or returns a hosted
        // checkout URL for the client to redirect to (stripe -> webhook grant).
        return billingProvider.createCheckout(institutionId, moduleService.getModule(moduleCode));
    }
}
