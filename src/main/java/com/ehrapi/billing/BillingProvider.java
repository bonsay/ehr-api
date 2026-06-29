package com.ehrapi.billing;

import com.ehrapi.entity.EhrModule;

/**
 * Abstraction over how a paid module is paid for. This mirrors the platform's
 * pluggable security modes: a {@code local} provider grants entitlements directly
 * (self-contained, ideal for dev/demo), while a {@code stripe} provider runs a
 * real hosted checkout and grants entitlements asynchronously via webhook.
 *
 * <p>The rest of the application depends only on this interface, so swapping or
 * adding providers needs no controller/service changes.
 */
public interface BillingProvider {

    /** The active billing mode identifier, e.g. {@code "local"} or {@code "stripe"}. */
    String mode();

    /**
     * Begin purchasing {@code module} for the given institution. Either grants the
     * entitlement immediately (returning {@link CheckoutResult#completed}) or
     * returns a hosted-checkout URL to redirect the buyer to.
     */
    CheckoutResult createCheckout(Long institutionId, EhrModule module);

    /**
     * Handle an inbound billing webhook (provider-specific payload). No-op for
     * providers that grant synchronously. Implementations MUST verify authenticity
     * before acting on the event.
     */
    default void handleWebhook(String payload, String signatureHeader) {
        // Providers without asynchronous settlement have nothing to do.
    }
}
