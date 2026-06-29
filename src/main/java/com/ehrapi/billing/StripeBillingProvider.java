package com.ehrapi.billing;

import com.ehrapi.entity.EhrModule;
import com.ehrapi.service.EntitlementService;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * {@code stripe} billing provider: starts a Stripe Checkout Session for the
 * module's configured price and grants the entitlement only once Stripe confirms
 * payment via a signed webhook. Activated with {@code ehr.billing.mode=stripe}
 * and the Stripe secret/webhook keys.
 */
@Component
@ConditionalOnProperty(name = "ehr.billing.mode", havingValue = "stripe")
public class StripeBillingProvider implements BillingProvider {

    private static final Logger log = LoggerFactory.getLogger(StripeBillingProvider.class);

    private static final String META_INSTITUTION = "institutionId";
    private static final String META_MODULE = "moduleCode";

    private final EntitlementService entitlementService;
    private final String webhookSecret;
    private final String successUrl;
    private final String cancelUrl;

    public StripeBillingProvider(EntitlementService entitlementService,
                                 @Value("${ehr.billing.stripe.secret-key:}") String secretKey,
                                 @Value("${ehr.billing.stripe.webhook-secret:}") String webhookSecret,
                                 @Value("${ehr.billing.success-url:http://localhost:4200/marketplace}") String successUrl,
                                 @Value("${ehr.billing.cancel-url:http://localhost:4200/marketplace}") String cancelUrl) {
        this.entitlementService = entitlementService;
        this.webhookSecret = webhookSecret;
        this.successUrl = successUrl;
        this.cancelUrl = cancelUrl;
        Stripe.apiKey = secretKey;
    }

    @Override
    public String mode() {
        return "stripe";
    }

    @Override
    public CheckoutResult createCheckout(Long institutionId, EhrModule module) {
        String priceId = module.getBillingPriceId();
        if (priceId == null || priceId.isBlank()) {
            throw new IllegalStateException(
                    "Module '" + module.getCode() + "' has no billing price configured.");
        }

        Map<String, String> metadata = Map.of(
                META_INSTITUTION, String.valueOf(institutionId),
                META_MODULE, module.getCode());

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .setClientReferenceId(institutionId + ":" + module.getCode())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(priceId)
                        .setQuantity(1L)
                        .build())
                .putAllMetadata(metadata)
                // Propagate metadata onto the subscription so cancellation events
                // can be mapped back to the institution + module.
                .setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                        .putAllMetadata(metadata)
                        .build())
                .build();

        try {
            Session session = Session.create(params);
            return CheckoutResult.redirect(mode(), session.getUrl());
        } catch (StripeException e) {
            log.error("Stripe checkout creation failed for institution {} module {}",
                    institutionId, module.getCode(), e);
            throw new IllegalStateException("Could not start checkout: " + e.getMessage(), e);
        }
    }

    @Override
    public void handleWebhook(String payload, String signatureHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            // Reject spoofed/garbled events.
            throw new IllegalArgumentException("Invalid Stripe webhook signature.");
        }

        StripeObject object = event.getDataObjectDeserializer().getObject().orElse(null);
        switch (event.getType()) {
            case "checkout.session.completed" -> {
                if (object instanceof Session session) {
                    apply(session.getMetadata(), Resolution.ACTIVATE);
                }
            }
            case "customer.subscription.deleted" -> {
                if (object instanceof Subscription subscription) {
                    apply(subscription.getMetadata(), Resolution.EXPIRE);
                }
            }
            default -> log.debug("Ignoring unhandled Stripe event type {}", event.getType());
        }
    }

    private void apply(Map<String, String> metadata, Resolution resolution) {
        if (metadata == null) {
            log.warn("Stripe event missing metadata; cannot map to an entitlement.");
            return;
        }
        String institution = metadata.get(META_INSTITUTION);
        String moduleCode = metadata.get(META_MODULE);
        if (institution == null || moduleCode == null) {
            log.warn("Stripe event metadata incomplete (institution={}, module={}).", institution, moduleCode);
            return;
        }
        Long institutionId = Long.valueOf(institution);
        if (resolution == Resolution.ACTIVATE) {
            entitlementService.activate(institutionId, moduleCode, "STRIPE", null);
            log.info("Activated entitlement for institution {} module {} from Stripe.", institutionId, moduleCode);
        } else {
            entitlementService.expire(institutionId, moduleCode);
            log.info("Expired entitlement for institution {} module {} from Stripe.", institutionId, moduleCode);
        }
    }

    private enum Resolution { ACTIVATE, EXPIRE }
}
