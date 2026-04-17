package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.Confidence;
import org.fiftieshousewife.cleancode.core.Finding;
import org.fiftieshousewife.cleancode.core.Severity;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class SurefireTestCaseFinding {

    private static final double SLOW_TEST_WARNING_SECONDS = 5.0;
    private static final double SLOW_TEST_ERROR_SECONDS = 30.0;

    private SurefireTestCaseFinding() {
    }

    static List<Finding> findingsForSuite(final Document doc) {
        final List<Finding> findings = new ArrayList<>();
        final NodeList testcases = doc.getElementsByTagName("testcase");

        for (int i = 0; i < testcases.getLength(); i++) {
            final Element testcase = (Element) testcases.item(i);
            findingForTestCase(testcase).ifPresent(findings::add);
        }

        return findings;
    }

    static Optional<Finding> findingForTestCase(final Element testcase) {
        final String testName = testcase.getAttribute("name");
        final String className = testcase.getAttribute("classname");
        final String sourceFile = className.replace('.', '/') + ".java";

        if (isSkipped(testcase)) {
            return Optional.of(Finding.at(
                    HeuristicCode.T3, sourceFile, -1, -1,
                    String.format("Test '%s' is skipped", testName),
                    Severity.INFO, Confidence.HIGH, "surefire", "skipped-test"));
        }

        return findingForSlowTest(testcase, testName, sourceFile);
    }

    static Optional<Finding> findingForSlowTest(final Element testcase, final String testName, final String sourceFile) {
        final double time = Double.parseDouble(testcase.getAttribute("time"));

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

    private static boolean isSkipped(final Element testcase) {
        return testcase.getElementsByTagName("skipped").getLength() > 0;
    }
}
