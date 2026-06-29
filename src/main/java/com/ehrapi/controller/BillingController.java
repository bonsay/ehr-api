package com.ehrapi.controller;

import com.ehrapi.billing.BillingProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Billing-provider integration surface: exposes the active mode and receives
 * provider webhooks (e.g. Stripe payment confirmations). The webhook is public
 * and authenticated by the provider's own signature verification, not by a token.
 */
@RestController
@RequestMapping("/api/billing")
@Tag(name = "Billing", description = "Billing provider mode and webhooks")
public class BillingController {

    private final BillingProvider billingProvider;

    public BillingController(BillingProvider billingProvider) {
        this.billingProvider = billingProvider;
    }

    @GetMapping("/mode")
    @Operation(summary = "The active billing mode (local / stripe)")
    public Map<String, String> mode() {
        return Map.of("mode", billingProvider.mode());
    }

    @PostMapping("/webhook")
    @Operation(summary = "Receive a billing provider webhook (signature-verified)")
    public ResponseEntity<String> webhook(@RequestBody String payload,
                                          @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        billingProvider.handleWebhook(payload, signature);
        return ResponseEntity.ok("ok");
    }
}
