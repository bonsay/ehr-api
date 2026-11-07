package com.bonsay.ai.gateway.routing;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

/** Model ids and per-token prices per tier, loaded from {@code pricing.properties}. */
public final class PricingTable {

    public record Price(String model, double inputPerMTok, double outputPerMTok) {
        public double cost(int inputTokens, int outputTokens) {
            return (inputTokens * inputPerMTok + outputTokens * outputPerMTok) / 1_000_000.0;
        }
    }

    private final Map<ModelTier, Price> prices;

    public PricingTable(Map<ModelTier, Price> prices) {
        this.prices = new EnumMap<>(prices);
    }

    public static PricingTable loadDefault() {
        Properties p = new Properties();
        try (InputStream in = PricingTable.class.getResourceAsStream("/pricing.properties")) {
            p.load(in);
        } catch (IOException | NullPointerException e) {
            throw new IllegalStateException("pricing.properties missing", e);
        }
        Map<ModelTier, Price> m = new EnumMap<>(ModelTier.class);
        for (ModelTier t : ModelTier.values()) {
            m.put(t, new Price(p.getProperty(t + ".model"), Double.parseDouble(p.getProperty(t + ".input")),
                    Double.parseDouble(p.getProperty(t + ".output"))));
        }
        return new PricingTable(m);
    }

    public Price price(ModelTier tier) { return prices.get(tier); }
}
