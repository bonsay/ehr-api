package com.bonsay.ai.gateway.limits;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * Per-tenant spend tracking. Above {@code softLimitRatio} of budget the gateway degrades to a
 * cheaper tier; at 100% requests are rejected. (Resetting per billing period is left to a scheduler.)
 */
public class BudgetManager {

    public enum Status { OK, SOFT_LIMIT, EXHAUSTED }

    private final Map<String, Double> budgets;
    private final double defaultBudget;
    private final double softLimitRatio;
    private final Map<String, DoubleAdder> spend = new ConcurrentHashMap<>();

    public BudgetManager(Map<String, Double> budgetsUsd, double defaultBudgetUsd, double softLimitRatio) {
        this.budgets = Map.copyOf(budgetsUsd);
        this.defaultBudget = defaultBudgetUsd;
        this.softLimitRatio = softLimitRatio;
    }

    public Status status(String tenant) {
        double used = spent(tenant);
        double budget = budgets.getOrDefault(tenant, defaultBudget);
        if (used >= budget) return Status.EXHAUSTED;
        return used >= budget * softLimitRatio ? Status.SOFT_LIMIT : Status.OK;
    }

    public void record(String tenant, double usd) {
        spend.computeIfAbsent(tenant, t -> new DoubleAdder()).add(usd);
    }

    public double spent(String tenant) {
        DoubleAdder a = spend.get(tenant);
        return a == null ? 0 : a.sum();
    }
}
