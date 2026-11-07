package com.bonsay.ai.gateway.routing;

/** Capability/cost tiers; concrete model ids and prices come from {@link PricingTable}. */
public enum ModelTier {
    FAST, BALANCED, FRONTIER;

    public ModelTier downgrade() {
        return this == FAST ? FAST : values()[ordinal() - 1];
    }
}
