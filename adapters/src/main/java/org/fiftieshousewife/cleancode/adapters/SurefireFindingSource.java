package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.Confidence;
import org.fiftieshousewife.cleancode.core.Finding;
import org.fiftieshousewife.cleancode.core.FindingSource;
import org.fiftieshousewife.cleancode.core.FindingSourceException;
import org.fiftieshousewife.cleancode.core.ProjectContext;
import org.fiftieshousewife.cleancode.core.Severity;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class SurefireFindingSource implements FindingSource {

    private static final double SKIP_PERCENTAGE_THRESHOLD = 10.0;
    private static final String SUREFIRE_REPORT_GLOB = "TEST-*.xml";

    @Override
    public String id() {
        return "surefire";
    }

    @Override
    public String displayName() {
        return "Surefire";
    }

    @Override
    public Set<HeuristicCode> coveredCodes() {
        return Set.of(HeuristicCode.T3, HeuristicCode.T4, HeuristicCode.T9);
    }

    @Override
    public boolean isAvailable(final ProjectContext context) {
        final Path dir = reportsDir(context);
        if (!Files.isDirectory(dir)) {
            return false;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, SUREFIRE_REPORT_GLOB)) {
            return stream.iterator().hasNext();
        } catch (final IOException e) {
            return false;
        }
    }

    @Override
    public List<Finding> collectFindings(final ProjectContext context) throws FindingSourceException {
        final Path dir = reportsDir(context);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try {
            return aggregateReports(dir);
        } catch (final IOException | NumberFormatException e) {
            throw new FindingSourceException("Failed to parse Surefire reports: " + dir, e);
        }
    }

    private List<Finding> aggregateReports(final Path dir) throws IOException, FindingSourceException {
        final List<SuiteReport> suites = readSuites(dir);
        final List<Finding> findings = new ArrayList<>();
        suites.forEach(suite -> findings.addAll(suite.findings()));
        skipPercentageFinding(suites).ifPresent(findings::add);
        return findings;
    }

    private List<SuiteReport> readSuites(final Path dir) throws IOException, FindingSourceException {
        final List<SuiteReport> suites = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, SUREFIRE_REPORT_GLOB)) {
            for (final Path reportFile : stream) {
                suites.add(readSuite(reportFile));
            }
        }
        return suites;
    }

    private SuiteReport readSuite(final Path reportFile) throws FindingSourceException {
        final Document doc = XmlReportParser.parse(reportFile);
        final Element suite = doc.getDocumentElement();
        return new SuiteReport(
                Integer.parseInt(suite.getAttribute("tests")),
                Integer.parseInt(suite.getAttribute("skipped")),
                SurefireTestCaseFinding.findingsForSuite(doc));
    }

    Optional<Finding> skipPercentageFinding(final List<SuiteReport> suites) {
        final int totalTests = suites.stream().mapToInt(SuiteReport::tests).sum();
        if (totalTests <= 0) {
            return Optional.empty();
        }
        final int totalSkipped = suites.stream().mapToInt(SuiteReport::skipped).sum();
        final double skipPercentage = (totalSkipped * 100.0) / totalTests;
        if (skipPercentage <= SKIP_PERCENTAGE_THRESHOLD) {
            return Optional.empty();
        }
        return Optional.of(Finding.projectLevel(
                HeuristicCode.T3,
                String.format("%.1f%% of tests are skipped (%d/%d)",
                        skipPercentage, totalSkipped, totalTests),
                Severity.WARNING, Confidence.HIGH, "surefire", "skip-percentage"));
    }

    private Path reportsDir(final ProjectContext context) {
        return context.buildDir().resolve("test-results/test");
    }

    record SuiteReport(int tests, int skipped, List<Finding> findings) {
    }
}
