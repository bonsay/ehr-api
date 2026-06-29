package com.ehrapi.entity;

/**
 * Lifecycle of an institution's right to use a paid module.
 *
 * <ul>
 *   <li>{@code TRIAL} — time-limited evaluation; active until {@code expiresAt}.</li>
 *   <li>{@code ACTIVE} — paid and in good standing.</li>
 *   <li>{@code EXPIRED} — lapsed (trial ended or subscription not renewed).</li>
 *   <li>{@code CANCELLED} — deliberately ended by the institution or admin.</li>
 * </ul>
 */
public enum EntitlementStatus {
    TRIAL,
    ACTIVE,
    EXPIRED,
    CANCELLED
}
