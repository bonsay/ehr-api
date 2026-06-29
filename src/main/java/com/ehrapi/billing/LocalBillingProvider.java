package com.ehrapi.billing;

import com.ehrapi.entity.EhrModule;
import com.ehrapi.service.EntitlementService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default {@code local} billing provider: a self-contained "buy" that grants the
 * entitlement immediately with no external payment gateway — the commercial
 * counterpart to the platform's local auth mode. Keeps the marketplace fully
 * demonstrable without Stripe keys.
 */
@Component
@ConditionalOnProperty(name = "ehr.billing.mode", havingValue = "local", matchIfMissing = true)
public class LocalBillingProvider implements BillingProvider {

    private final EntitlementService entitlementService;

    public LocalBillingProvider(EntitlementService entitlementService) {
        this.entitlementService = entitlementService;
    }

    @Override
    public String mode() {
        return "local";
    }

    @Override
    public CheckoutResult createCheckout(Long institutionId, EhrModule module) {
        entitlementService.purchase(institutionId, module.getCode());
        return CheckoutResult.completed(mode());
    }
}
