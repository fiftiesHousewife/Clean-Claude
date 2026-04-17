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
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class SurefireFindingSource implements FindingSource {
    private static final double SLOW_TEST_WARNING_SECONDS = 5.0;
    private static final double SLOW_TEST_ERROR_SECONDS = 30.0;
    private static final double SKIP_PERCENTAGE_THRESHOLD = 10.0;
    private static final String REPORT_FILE_GLOB = "TEST-*.xml";

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
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, REPORT_FILE_GLOB)) {
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

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, REPORT_FILE_GLOB)) {
            final List<Finding> findings = new ArrayList<>();
            final SuiteCounts counts = new SuiteCounts();
            for (final Path reportFile : stream) {
                final Document doc = XmlReportParser.parse(reportFile);
                final Element suite = doc.getDocumentElement();
                counts.add(
                        Integer.parseInt(suite.getAttribute("tests")),
                        Integer.parseInt(suite.getAttribute("skipped")));
                findings.addAll(findingsForSuite(doc));
            }
            skipPercentageFinding(counts).ifPresent(findings::add);
            return findings;
        } catch (IOException | NumberFormatException e) {
            throw new FindingSourceException("Failed to parse Surefire reports: " + dir, e);
        }
    }

    List<Finding> findingsForSuite(Document doc) {
        final List<Finding> findings = new ArrayList<>();
        final NodeList testcases = doc.getElementsByTagName("testcase");

        for (int i = 0; i < testcases.getLength(); i++) {
            final Element testcase = (Element) testcases.item(i);
            findingForTestCase(testcase).ifPresent(findings::add);
        }

        return findings;
    }

    Optional<Finding> findingForTestCase(Element testcase) {
        final String testName = testcase.getAttribute("name");
        final String className = testcase.getAttribute("classname");
        final String sourceFile = className.replace('.', '/') + ".java";

        if (isSkipped(testcase)) {
            return Optional.of(Finding.at(
                    HeuristicCode.T3, sourceFile, -1, -1,
                    String.format("Test '%s' is skipped", testName),
                    Severity.INFO, Confidence.HIGH, "surefire", "skipped-test"));
        }

        return findingForSlowTest(new SlowTestContext(testcase, testName, sourceFile));
    }

    Optional<Finding> findingForSlowTest(SlowTestContext context) {
        final double time = Double.parseDouble(context.testcase().getAttribute("time"));
        return slowTestThreshold(time)
                .map(threshold -> slowTestFinding(context, time, threshold));
    }

    private Optional<SlowTestThreshold> slowTestThreshold(double time) {
        if (time > SLOW_TEST_ERROR_SECONDS) {
            return Optional.of(new SlowTestThreshold(SLOW_TEST_ERROR_SECONDS, Severity.ERROR));
        }
        if (time > SLOW_TEST_WARNING_SECONDS) {
            return Optional.of(new SlowTestThreshold(SLOW_TEST_WARNING_SECONDS, Severity.WARNING));
        }
        return Optional.empty();
    }

    Finding slowTestFinding(SlowTestContext context, double time, SlowTestThreshold threshold) {
        return Finding.at(
                HeuristicCode.T9, context.sourceFile(), -1, -1,
                String.format("Test '%s' took %.1fs (exceeds %.0fs threshold)",
                        context.testName(), time, threshold.seconds()),
                threshold.severity(), Confidence.HIGH, "surefire", "slow-test");
    }

    Optional<Finding> skipPercentageFinding(SuiteCounts counts) {
        if (counts.totalTests() <= 0) {
            return Optional.empty();
        }

        final double skipPercentage = (counts.totalSkipped() * 100.0) / counts.totalTests();
        if (skipPercentage <= SKIP_PERCENTAGE_THRESHOLD) {
            return Optional.empty();
        }
        return Optional.of(Finding.projectLevel(
                HeuristicCode.T3,
                String.format("%.1f%% of tests are skipped (%d/%d)",
                        skipPercentage, counts.totalSkipped(), counts.totalTests()),
                Severity.WARNING, Confidence.HIGH, "surefire", "skip-percentage"));
    }

    private boolean isSkipped(Element testcase) {
        return testcase.getElementsByTagName("skipped").getLength() > 0;
    }

    private Path reportsDir(ProjectContext context) {
        return context.buildDir().resolve("test-results/test");
    }

    record SlowTestContext(Element testcase, String testName, String sourceFile) {}

    record SlowTestThreshold(double seconds, Severity severity) {}

    static final class SuiteCounts {
        private int totalTests;
        private int totalSkipped;

        void add(int tests, int skipped) {
            this.totalTests += tests;
            this.totalSkipped += skipped;
        }

        int totalTests() {
            return totalTests;
        }

        int totalSkipped() {
            return totalSkipped;
        }
    }
}
