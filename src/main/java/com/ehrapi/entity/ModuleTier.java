package com.ehrapi.entity;

/**
 * Commercial tier of a catalog module. {@code FREE} modules are the basic
 * clinical baseline available to every institution; {@code PRO} and
 * {@code ENTERPRISE} modules require an active {@link ModuleEntitlement} before
 * they can be enabled — the "basic free, advanced purchasable" model.
 */
public enum ModuleTier {
    FREE,
    PRO,
    ENTERPRISE
}
