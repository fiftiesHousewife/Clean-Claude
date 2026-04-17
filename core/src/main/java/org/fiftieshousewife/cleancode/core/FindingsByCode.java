package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

final class FindingsByCode {

    private FindingsByCode() {}

    static void forEachSorted(final List<Finding> findings,
                              final BiConsumer<HeuristicCode, List<Finding>> action) {
        final Map<HeuristicCode, List<Finding>> byCode = findings.stream()
                .collect(Collectors.groupingBy(Finding::code));

        byCode.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().name()))
                .forEach(entry -> action.accept(entry.getKey(), entry.getValue()));
    }
}
