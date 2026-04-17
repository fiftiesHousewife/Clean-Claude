package org.fiftieshousewife.cleancode.plugin;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.AggregatedReport;
import org.fiftieshousewife.cleancode.core.BaselineManager;
import org.fiftieshousewife.cleancode.core.BuildOutputFormatter;
import org.fiftieshousewife.cleancode.core.HtmlReportWriter;
import org.fiftieshousewife.cleancode.core.JsonReportWriter;
import org.gradle.api.logging.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

final class AnalyseOutputs {

    private static final String FINDINGS_JSON = "findings.json";
    private static final String BLOB_MAIN = "/blob/main";

    private final Logger logger;
    private final String repositoryUrl;

    AnalyseOutputs(final Logger logger,
                   final String baseRepositoryUrl,
                   final Path rootDir,
                   final Path projectRoot) {
        this.logger = logger;
        this.repositoryUrl = resolveRepositoryUrl(baseRepositoryUrl, rootDir, projectRoot);
    }

    void writeReports(final AggregatedReport report, final Path outputDir, final Path htmlReport) throws Exception {
        JsonReportWriter.write(report, outputDir.resolve(FINDINGS_JSON));
        HtmlReportWriter.write(report, htmlReport, repositoryUrl);
    }

    void logReport(final AggregatedReport report, final Path baselineFile, final Path htmlReport) throws Exception {
        final Map<HeuristicCode, BaselineManager.Delta> deltas = Files.exists(baselineFile)
                ? BaselineManager.computeDeltas(report, baselineFile)
                : Map.of();
        logger.lifecycle(BuildOutputFormatter.format(report, deltas));
        logger.lifecycle("\n  Report: file://" + htmlReport.toAbsolutePath());
    }

    private static String resolveRepositoryUrl(final String baseRepositoryUrl,
                                               final Path rootDir,
                                               final Path projectRoot) {
        if (baseRepositoryUrl.isBlank()) {
            return "";
        }
        final String modulePath = rootDir.relativize(projectRoot).toString();
        return baseRepositoryUrl + BLOB_MAIN + (modulePath.isEmpty() ? "" : "/" + modulePath);
    }
}
