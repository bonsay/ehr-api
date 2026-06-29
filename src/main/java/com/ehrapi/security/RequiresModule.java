package com.ehrapi.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller (or handler method) as belonging to a catalog module, so
 * the {@link ModuleEntitlementInterceptor} can refuse access when the caller's
 * institution has no active entitlement for that module (HTTP 402).
 *
 * <p>This is the commercial sibling of {@code @PreAuthorize}: {@code @PreAuthorize}
 * answers "is this user allowed?", {@code @RequiresModule} answers "has this
 * institution licensed this capability?". FREE modules always pass, so annotating
 * a free controller is harmless and future-proofs it should it ever become paid.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresModule {
    /** The catalog module code this endpoint belongs to (see ModuleCodes). */
    String value();
}
