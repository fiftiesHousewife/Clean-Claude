package org.fiftieshousewife.cleancode.plugin;

import org.fiftieshousewife.cleancode.core.AggregatedReport;
import org.fiftieshousewife.cleancode.core.Finding;
import org.fiftieshousewife.cleancode.core.FindingFilter;
import org.fiftieshousewife.cleancode.core.PackageSuppression;
import org.fiftieshousewife.cleancode.core.SuppressionIndex;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

final class AnalyseFilters {

    private final Path mainSources;

    AnalyseFilters(final Path mainSources) {
        this.mainSources = mainSources;
    }

    AggregatedReport apply(final AggregatedReport report,
                           final Set<String> disabledRecipes,
                           final PackageSuppression packageSuppression) {
        final AggregatedReport filteredByCode = filterDisabledRecipes(report, disabledRecipes);
        return filterSuppressions(filteredByCode, packageSuppression);
    }

    private AggregatedReport filterDisabledRecipes(final AggregatedReport report, final Set<String> disabledRecipes) {
        if (disabledRecipes.isEmpty()) {
            return report;
        }
        final List<Finding> filtered = report.findings().stream()
                .filter(f -> !disabledRecipes.contains(f.code().name()))
                .toList();
        return withFindings(report, filtered);
    }

    private AggregatedReport filterSuppressions(final AggregatedReport report,
                                                final PackageSuppression packageSuppression) {
        final SuppressionIndex index = SuppressionIndex.build(mainSources);
        final FindingFilter.Result result = FindingFilter.apply(report.findings(), index, packageSuppression);
        return withFindings(report, result.findings());
    }

    private static AggregatedReport withFindings(final AggregatedReport report, final List<Finding> findings) {
        return new AggregatedReport(
                findings,
                report.coveredCodes(),
                report.generatedAt(),
                report.projectName(),
                report.projectVersion());
    }
}
