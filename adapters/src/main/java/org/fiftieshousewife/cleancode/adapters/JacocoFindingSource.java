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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class JacocoFindingSource implements FindingSource {

    private static final int MIN_CLASS_LINES = 10;
    private static final String LINE_COUNTER_TYPE = "LINE";
    private static final String COUNTER_TAG = "counter";
    private static final double CLASS_COVERAGE_WARNING_THRESHOLD = 50.0;
    private static final double COVERAGE_ERROR_THRESHOLD = 50.0;
    private static final double COVERAGE_WARNING_THRESHOLD = 75.0;

    @Override
    public String id() {
        return "jacoco";
    }

    @Override
    public String displayName() {
        return "JaCoCo";
    }

    @Override
    public Set<HeuristicCode> coveredCodes() {
        return Set.of(HeuristicCode.T1, HeuristicCode.T2, HeuristicCode.T8);
    }

    @Override
    public boolean isAvailable(final ProjectContext context) {
        return true;
    }

    @Override
    public List<Finding> collectFindings(final ProjectContext context) throws FindingSourceException {
        final Path report = reportPath(context);
        if (!Files.exists(report)) {
            return missingReportFindings(context);
        }
        final Document doc = XmlReportParser.parse(report);
        final List<Finding> findings = new ArrayList<>(projectLevelCoverage(doc));
        findings.addAll(perClassFindings(doc));
        return findings;
    }

    private List<Finding> missingReportFindings(final ProjectContext context) {
        final boolean hasTestSources = context.testSourceRoots().stream().anyMatch(Files::exists);
        if (!hasTestSources) {
            return List.of();
        }
        return List.of(Finding.projectLevel(HeuristicCode.T2,
                "JaCoCo report not found — coverage tool not configured",
                Severity.ERROR, Confidence.HIGH, "jacoco", "missing-report"));
    }

    private List<Finding> projectLevelCoverage(final Document doc) {
        return reportLineCounter(doc).map(this::projectCoverageFinding).map(List::of).orElseGet(List::of);
    }

    private Optional<Element> reportLineCounter(final Document doc) {
        final NodeList counters = doc.getDocumentElement().getChildNodes();
        for (int i = 0; i < counters.getLength(); i++) {
            if (isLineCounter(counters.item(i))) {
                return Optional.of((Element) counters.item(i));
            }
        }
        return Optional.empty();
    }

    private boolean isLineCounter(final Object node) {
        return node instanceof Element el
                && COUNTER_TAG.equals(el.getTagName())
                && LINE_COUNTER_TYPE.equals(el.getAttribute("type"));
    }

    private Finding projectCoverageFinding(final Element counter) {
        final LineCoverage coverage = LineCoverage.from(counter);
        return Finding.projectLevel(HeuristicCode.T1,
                String.format("Overall line coverage: %.1f%% (%d/%d lines covered)",
                        coverage.percentage(), coverage.covered(), coverage.total()),
                coverageSeverity(coverage.percentage()), Confidence.HIGH, "jacoco", "line-coverage");
    }

    private List<Finding> perClassFindings(final Document doc) {
        final NodeList classNodes = doc.getElementsByTagName("class");
        final List<Finding> findings = new ArrayList<>();
        for (int i = 0; i < classNodes.getLength(); i++) {
            classFinding((Element) classNodes.item(i)).ifPresent(findings::add);
        }
        return findings;
    }

    private Optional<Finding> classFinding(final Element classElement) {
        return classLineCounter(classElement)
                .map(LineCoverage::from)
                .filter(c -> c.total() >= MIN_CLASS_LINES)
                .filter(c -> c.percentage() < CLASS_COVERAGE_WARNING_THRESHOLD)
                .map(c -> classCoverageFinding(classElement, c));
    }

    private Optional<Element> classLineCounter(final Element classElement) {
        final NodeList counters = classElement.getElementsByTagName(COUNTER_TAG);
        for (int j = 0; j < counters.getLength(); j++) {
            final Element counter = (Element) counters.item(j);
            if (LINE_COUNTER_TYPE.equals(counter.getAttribute("type"))) {
                return Optional.of(counter);
            }
        }
        return Optional.empty();
    }

    private Finding classCoverageFinding(final Element classElement, final LineCoverage coverage) {
        final String className = classElement.getAttribute("name");
        final int simpleNameStart = className.lastIndexOf('/') + 1;
        final String packagePath = className.substring(0, simpleNameStart - 1);
        final String simpleName = className.substring(simpleNameStart);
        final String sourcePath = packagePath + "/" + classElement.getAttribute("sourcefilename");
        return Finding.at(HeuristicCode.T8, sourcePath, 1, -1,
                String.format("Class %s has %.1f%% line coverage (%d/%d lines)",
                        simpleName, coverage.percentage(), coverage.covered(), coverage.total()),
                Severity.WARNING, Confidence.MEDIUM, "jacoco", "class-coverage");
    }

    private Severity coverageSeverity(final double percentage) {
        if (percentage < COVERAGE_ERROR_THRESHOLD) {
            return Severity.ERROR;
        }
        return percentage < COVERAGE_WARNING_THRESHOLD ? Severity.WARNING : Severity.INFO;
    }

    private Path reportPath(final ProjectContext context) {
        return context.reportsDir().resolve("jacoco/test/jacocoTestReport.xml");
    }
}
