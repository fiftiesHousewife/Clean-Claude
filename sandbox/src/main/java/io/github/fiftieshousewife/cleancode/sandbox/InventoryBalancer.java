package io.github.fiftieshousewife.cleancode.sandbox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Imperative-loop aggregator — classic candidate for stream conversion
 * (G30 when the loop is long). Each loop body also has nested
 * conditionals that could simplify via Map.computeIfAbsent. Deliberately
 * mutation-heavy to exercise the LLM's pattern-matching on stream
 * rewrites, where recipes have less leverage.
 */
public final class InventoryBalancer {

    public Map<String, Integer> balanceByRegion(final List<Lot> lots) {
        final Map<String, Integer> balances = new HashMap<>();
        for (final Lot lot : lots) {
            if (lot == null) {
                continue;
            }
            if (lot.region() == null || lot.region().isBlank()) {
                continue;
            }
            if (lot.quantity() < 0) {
                continue;
            }
            if (balances.containsKey(lot.region())) {
                final int existing = balances.get(lot.region());
                balances.put(lot.region(), existing + lot.quantity());
            } else {
                balances.put(lot.region(), lot.quantity());
            }
        }
        return balances;
    }

    public List<String> overstockedRegions(final Map<String, Integer> balances, final int threshold) {
        final List<String> overstocked = new ArrayList<>();
        for (final Map.Entry<String, Integer> entry : balances.entrySet()) {
            if (entry.getValue() > threshold) {
                overstocked.add(entry.getKey());
            }
        }
        return overstocked;
    }

    public int totalAcrossRegions(final Map<String, Integer> balances) {
        int sum = 0;
        for (final Integer value : balances.values()) {
            if (value != null) {
                sum = sum + value;
            }
        }
        return sum;
    }

    public record Lot(String region, String sku, int quantity) {}
}
