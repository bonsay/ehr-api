package com.ehrapi.billing;

/**
 * Outcome of starting a purchase.
 *
 * <ul>
 *   <li>{@code completed == true} — the entitlement was granted synchronously
 *       (local provider); the client just refreshes module state.</li>
 *   <li>{@code checkoutUrl != null} — the client must redirect the user to a
 *       hosted checkout (Stripe); the entitlement is granted later by webhook.</li>
 * </ul>
 *
 * @param mode        the active billing mode ("local" / "stripe")
 * @param completed   whether the purchase is already fully applied
 * @param checkoutUrl hosted checkout URL to redirect to, or null
 */
public record CheckoutResult(String mode, boolean completed, String checkoutUrl) {

    public static CheckoutResult completed(String mode) {
        return new CheckoutResult(mode, true, null);
    }

    public static CheckoutResult redirect(String mode, String checkoutUrl) {
        return new CheckoutResult(mode, false, checkoutUrl);
    }
}
