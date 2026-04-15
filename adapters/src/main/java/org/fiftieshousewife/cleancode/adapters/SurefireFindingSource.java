package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SurefireFindingSource implements FindingSource {

    private static final double SLOW_TEST_WARNING_SECONDS = 5.0;
    private static final double SLOW_TEST_ERROR_SECONDS = 30.0;
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
        Path dir = reportsDir(context);
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
        Path dir = reportsDir(context);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }

        try {
            List<Finding> findings = new ArrayList<>();
            int totalTests = 0;
            int totalSkipped = 0;

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "TEST-*.xml")) {
                for (Path reportFile : stream) {
                    Document doc = XmlReportParser.parse(reportFile);
                    Element suite = doc.getDocumentElement();

                    totalTests += Integer.parseInt(suite.getAttribute("tests"));
                    totalSkipped += Integer.parseInt(suite.getAttribute("skipped"));

                    findings.addAll(findingsForSuite(doc));
                }
            }

            addSkipPercentageFinding(findings, totalSkipped, totalTests);
            return findings;
        } catch (Exception e) {
            throw new FindingSourceException("Failed to parse Surefire reports: " + dir, e);
        }
    }

    List<Finding> findingsForSuite(Document doc) {
        List<Finding> findings = new ArrayList<>();
        NodeList testcases = doc.getElementsByTagName("testcase");

        for (int i = 0; i < testcases.getLength(); i++) {
            Element testcase = (Element) testcases.item(i);
            findingForTestCase(testcase).ifPresent(findings::add);
        }

        return findings;
    }

    Optional<Finding> findingForTestCase(Element testcase) {
        String testName = testcase.getAttribute("name");
        String className = testcase.getAttribute("classname");
        String sourceFile = className.replace('.', '/') + ".java";

        if (isSkipped(testcase)) {
            return Optional.of(Finding.at(
                    HeuristicCode.T3, sourceFile, -1, -1,
                    String.format("Test '%s' is skipped", testName),
                    Severity.INFO, Confidence.HIGH, "surefire", "skipped-test"));
        }

        return findingForSlowTest(testcase, testName, sourceFile);
    }

    Optional<Finding> findingForSlowTest(Element testcase, String testName, String sourceFile) {
        double time = Double.parseDouble(testcase.getAttribute("time"));

        if (time > SLOW_TEST_ERROR_SECONDS) {
            return Optional.of(Finding.at(
                    HeuristicCode.T9, sourceFile, -1, -1,
                    String.format("Test '%s' took %.1fs (exceeds %.0fs threshold)",
                            testName, time, SLOW_TEST_ERROR_SECONDS),
                    Severity.ERROR, Confidence.HIGH, "surefire", "slow-test"));
        }

        if (time > SLOW_TEST_WARNING_SECONDS) {
            return Optional.of(Finding.at(
                    HeuristicCode.T9, sourceFile, -1, -1,
                    String.format("Test '%s' took %.1fs (exceeds %.0fs threshold)",
                            testName, time, SLOW_TEST_WARNING_SECONDS),
                    Severity.WARNING, Confidence.HIGH, "surefire", "slow-test"));
        }

        return Optional.empty();
    }

    void addSkipPercentageFinding(List<Finding> findings, int totalSkipped, int totalTests) {
        if (totalTests <= 0) {
            return;
        }

        double skipPercentage = (totalSkipped * 100.0) / totalTests;
        if (skipPercentage > SKIP_PERCENTAGE_THRESHOLD) {
            findings.add(Finding.projectLevel(
                    HeuristicCode.T3,
                    String.format("%.1f%% of tests are skipped (%d/%d)",
                            skipPercentage, totalSkipped, totalTests),
                    Severity.WARNING, Confidence.HIGH, "surefire", "skip-percentage"));
        }
    }

    private boolean isSkipped(Element testcase) {
        return testcase.getElementsByTagName("skipped").getLength() > 0;
    }

    private Path reportsDir(ProjectContext context) {
        return context.buildDir().resolve("test-results/test");
    }
}
