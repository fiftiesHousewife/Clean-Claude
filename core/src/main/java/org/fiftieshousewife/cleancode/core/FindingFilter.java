package org.fiftieshousewife.cleancode.core;

import java.util.ArrayList;
import java.util.List;

public final class FindingFilter {

    private FindingFilter() {}

    public record Result(List<Finding> findings) {}

    public static Result apply(List<Finding> findings, SuppressionIndex index) {
        List<Finding> filtered = new ArrayList<>();

        for (Finding f : findings) {
            if ("spotbugs".equals(f.tool()) || !index.isSuppressed(f)) {
                filtered.add(f);
            }
        }

        filtered.addAll(index.metaFindings());

        return new Result(List.copyOf(filtered));
    }
}
