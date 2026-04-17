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
import java.util.Set;

public class SurefireFindingSource implements FindingSource {

    private static final double SKIP_PERCENTAGE_THRESHOLD = 10.0;

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
    public boolean isAvailable(ProjectContext context) {
        final Path dir = reportsDir(context);
        if (!Files.isDirectory(dir)) {
            return false;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "TEST-*.xml")) {
            return stream.iterator().hasNext();
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public List<Finding> collectFindings(ProjectContext context) throws FindingSourceException {
        final Path dir = reportsDir(context);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }

        try {
            final List<Finding> findings = new ArrayList<>();
            int totalTests = 0;
            int totalSkipped = 0;

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "TEST-*.xml")) {
                for (Path reportFile : stream) {
                    final Document doc = XmlReportParser.parse(reportFile);
                    final Element suite = doc.getDocumentElement();

                    totalTests += Integer.parseInt(suite.getAttribute("tests"));
                    totalSkipped += Integer.parseInt(suite.getAttribute("skipped"));

                    findings.addAll(SurefireTestCaseFinding.findingsForSuite(doc));
                }
            }

            addSkipPercentageFinding(findings, totalSkipped, totalTests);
            return findings;
        } catch (Exception e) {
            throw new FindingSourceException("Failed to parse Surefire reports: " + dir, e);
        }
    }

    void addSkipPercentageFinding(List<Finding> findings, int totalSkipped, int totalTests) {
        if (totalTests <= 0) {
            return;
        }

        final double skipPercentage = (totalSkipped * 100.0) / totalTests;
        if (skipPercentage > SKIP_PERCENTAGE_THRESHOLD) {
            findings.add(Finding.projectLevel(
                    HeuristicCode.T3,
                    String.format("%.1f%% of tests are skipped (%d/%d)",
                            skipPercentage, totalSkipped, totalTests),
                    Severity.WARNING, Confidence.HIGH, "surefire", "skip-percentage"));
        }
    }

    private Path reportsDir(ProjectContext context) {
        return context.buildDir().resolve("test-results/test");
    }
}
