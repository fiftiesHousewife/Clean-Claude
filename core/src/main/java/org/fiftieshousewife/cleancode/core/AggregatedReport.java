package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public record AggregatedReport(
        List<Finding> findings,
        Set<HeuristicCode> coveredCodes,
        Instant generatedAt,
        String projectName,
        String projectVersion
) {
    public Map<HeuristicCode, List<Finding>> byCode() {
        return findings.stream().collect(
                Collectors.groupingBy(Finding::code, TreeMap::new, Collectors.toList()));
    }

    public Map<Severity, List<Finding>> bySeverity() {
        return findings.stream().collect(
                Collectors.groupingBy(Finding::severity));
    }
}
