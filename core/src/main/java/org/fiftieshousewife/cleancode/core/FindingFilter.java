package org.fiftieshousewife.cleancode.core;

import java.util.ArrayList;
import java.util.List;

public final class FindingFilter {

    private FindingFilter() {}

    public record Result(List<Finding> findings) {}

    public static Result apply(final List<Finding> findings, final SuppressionIndex index) {
        return apply(findings, index, PackageSuppression.empty());
    }

    public static Result apply(final List<Finding> findings,
                               final SuppressionIndex index,
                               final PackageSuppression packageSuppression) {
        final List<Finding> filtered = new ArrayList<>();

        for (final Finding f : findings) {
            if (!index.isSuppressed(f) && !packageSuppression.suppresses(f)) {
                filtered.add(f);
            }
        }

        filtered.addAll(index.metaFindings());

        return new Result(List.copyOf(filtered));
    }
}
