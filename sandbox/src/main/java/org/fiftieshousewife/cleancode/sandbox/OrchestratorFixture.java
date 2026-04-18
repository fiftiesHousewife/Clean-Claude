package org.fiftieshousewife.cleancode.sandbox;

import java.util.List;
import java.util.Map;

/**
 * G30-heavy fixture: a single orchestrator method strung together from
 * six distinct phases. Each phase is 8-10 lines, has a clear name in the
 * comments, and does not cross-reference other phases — i.e. the
 * textbook shape for ExtractMethodRecipe. No F2/G18/G12/G31 noise;
 * extraction is the obvious right fix.
 */
public final class OrchestratorFixture {

    public void settleOrders(final List<String> orderIds, final Map<String, Integer> stock,
                             final Map<String, Double> prices, final StringBuilder audit) {
        // Phase 1: validate input collections.
        if (orderIds == null || orderIds.isEmpty()) {
            audit.append("no orders to settle\n");
            return;
        }
        if (stock == null || prices == null) {
            audit.append("missing reference data\n");
            return;
        }
        // Phase 2: dedupe the incoming ids into a clean working list.
        final java.util.LinkedHashSet<String> unique = new java.util.LinkedHashSet<>();
        for (final String id : orderIds) {
            if (id != null && !id.isBlank()) {
                unique.add(id.trim());
            }
        }
        audit.append("unique orders: ").append(unique.size()).append("\n");
        // Phase 3: apply a flat reservation discount to each line price.
        final java.util.LinkedHashMap<String, Double> adjusted = new java.util.LinkedHashMap<>();
        for (final String id : unique) {
            final Double base = prices.get(id);
            if (base == null) {
                audit.append("no price for ").append(id).append(" — skipping\n");
                continue;
            }
            adjusted.put(id, base * 0.95);
        }
        // Phase 4: decrement the stock counter for every priced order.
        int fulfilled = 0;
        for (final String id : adjusted.keySet()) {
            final Integer available = stock.get(id);
            if (available == null || available <= 0) {
                audit.append("out of stock: ").append(id).append("\n");
                continue;
            }
            stock.put(id, available - 1);
            fulfilled++;
        }
        audit.append("fulfilled: ").append(fulfilled).append("\n");
        // Phase 5: compute the total revenue from the fulfilled orders.
        double revenue = 0.0;
        for (final Map.Entry<String, Double> entry : adjusted.entrySet()) {
            final Integer available = stock.get(entry.getKey());
            if (available != null) {
                revenue += entry.getValue();
            }
        }
        audit.append("revenue: ").append(String.format("%.2f", revenue)).append("\n");
        // Phase 6: sign off.
        audit.append("settlement complete\n");
        audit.append("---\n");
    }
}
