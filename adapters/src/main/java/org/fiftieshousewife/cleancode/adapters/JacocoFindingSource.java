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
    public boolean isAvailable(ProjectContext context) {
        return true; // Always available — absence produces T2
    }

    @Override
    public List<Finding> collectFindings(ProjectContext context) throws FindingSourceException {
        final Path report = reportPath(context);
        if (!Files.exists(report)) {
            return handleMissingReport(context);
        }
        try {
            final Document doc = XmlReportParser.parse(report);
            final List<Finding> findings = new ArrayList<>();
            addProjectLevelCoverage(doc, findings);
            addPerClassFindings(doc, findings);
            return findings;
        } catch (Exception e) {
            throw new FindingSourceException("Failed to parse JaCoCo report: " + report, e);
        }
    }

    private List<Finding> handleMissingReport(ProjectContext context) {
        final boolean hasTestSources = context.testSourceRoots().stream().anyMatch(Files::exists);
        if (hasTestSources) {
            return List.of(Finding.projectLevel(
                    HeuristicCode.T2,
                    "JaCoCo report not found — coverage tool not configured",
                    Severity.ERROR, Confidence.HIGH, "jacoco", "missing-report"));
        }
        return List.of();
    }

    private void addProjectLevelCoverage(Document doc, List<Finding> findings) {
        final NodeList counters = doc.getDocumentElement().getChildNodes();
        for (int i = 0; i < counters.getLength(); i++) {
            if (counters.item(i) instanceof Element el && "counter".equals(el.getTagName())) {
                final Optional<LineCoverage> coverage = lineCoverage(el);
                if (coverage.isPresent()) {
                    findings.add(projectFinding(coverage.get()));
                    break;
                }
            }
        }
    }

    private Finding projectFinding(LineCoverage coverage) {
        return Finding.projectLevel(
                HeuristicCode.T1,
                String.format("Overall line coverage: %.1f%% (%d/%d lines covered)",
                        coverage.percentage(), coverage.covered(), coverage.total()),
                coverageSeverity(coverage.percentage()), Confidence.HIGH, "jacoco", "line-coverage");
    }

    private void addPerClassFindings(Document doc, List<Finding> findings) {
        final NodeList classNodes = doc.getElementsByTagName("class");
        for (int i = 0; i < classNodes.getLength(); i++) {
            final Element classElement = (Element) classNodes.item(i);
            final NodeList counters = classElement.getElementsByTagName("counter");
            for (int j = 0; j < counters.getLength(); j++) {
                final Optional<LineCoverage> coverage = lineCoverage((Element) counters.item(j));
                if (coverage.isPresent()) {
                    coverage.filter(c -> c.total() >= MIN_CLASS_LINES && c.percentage() < 50)
                            .ifPresent(c -> findings.add(classFinding(classElement, c)));
                    break;
                }
            }
        }
    }

    private Finding classFinding(Element classElement, LineCoverage coverage) {
        final String className = classElement.getAttribute("name");
        final String sourceFile = classElement.getAttribute("sourcefilename");
        final String packagePath = className.substring(0, className.lastIndexOf('/'));
        final String simpleName = className.substring(className.lastIndexOf('/') + 1);
        return Finding.at(
                HeuristicCode.T8,
                packagePath + "/" + sourceFile,
                1, -1,
                String.format("Class %s has %.1f%% line coverage (%d/%d lines)",
                        simpleName, coverage.percentage(), coverage.covered(), coverage.total()),
                Severity.WARNING, Confidence.MEDIUM, "jacoco", "class-coverage");
    }

    private Optional<LineCoverage> lineCoverage(Element counter) {
        if (!"LINE".equals(counter.getAttribute("type"))) {
            return Optional.empty();
        }
        final int missed = Integer.parseInt(counter.getAttribute("missed"));
        final int covered = Integer.parseInt(counter.getAttribute("covered"));
        return Optional.of(new LineCoverage(covered, missed + covered));
    }

    private Severity coverageSeverity(double percentage) {
        if (percentage < 50) {
            return Severity.ERROR;
        }
        return percentage < 75 ? Severity.WARNING : Severity.INFO;
    }

    private Path reportPath(ProjectContext context) {
        return context.reportsDir().resolve("jacoco/test/jacocoTestReport.xml");
    }

    private record LineCoverage(int covered, int total) {
        double percentage() {
            return total > 0 ? (covered * 100.0) / total : 0;
        }
    }
}
