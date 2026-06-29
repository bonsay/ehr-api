package com.ehrapi.entity;

/**
 * How a module is paid for. {@code FREE} carries no charge; {@code SUBSCRIPTION}
 * is a recurring (monthly) fee; {@code ONE_TIME} is a single purchase. The price
 * itself is carried by {@link EhrModule#getPriceMonthlyCents()}.
 */
public enum PriceModel {
    FREE,
    SUBSCRIPTION,
    ONE_TIME
}
