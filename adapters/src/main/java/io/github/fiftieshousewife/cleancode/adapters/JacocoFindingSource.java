package io.github.fiftieshousewife.cleancode.adapters;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;
import io.github.fiftieshousewife.cleancode.core.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

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
        Path report = reportPath(context);

        if (!Files.exists(report)) {
            return handleMissingReport(context);
        }

        try {
            Document doc = XmlReportParser.parse(report);

            List<Finding> findings = new ArrayList<>();

            // Project-level T1 finding
            addProjectLevelCoverage(doc, findings);

            // Per-class T8 findings
            addPerClassFindings(doc, findings);

            return findings;
        } catch (Exception e) {
            throw new FindingSourceException("Failed to parse JaCoCo report: " + report, e);
        }
    }

    private List<Finding> handleMissingReport(ProjectContext context) {
        boolean hasTestSources = context.testSourceRoots().stream()
                .anyMatch(Files::exists);

        if (hasTestSources) {
            return List.of(Finding.projectLevel(
                    HeuristicCode.T2,
                    "JaCoCo report not found — coverage tool not configured",
                    Severity.ERROR, Confidence.HIGH, "jacoco", "missing-report"));
        }
        return List.of();
    }

    private void addProjectLevelCoverage(Document doc, List<Finding> findings) {
        // Find the report-level LINE counter (direct child of <report>)
        Element reportElement = doc.getDocumentElement();
        NodeList counters = reportElement.getChildNodes();

        for (int i = 0; i < counters.getLength(); i++) {
            if (counters.item(i) instanceof Element el
                    && "counter".equals(el.getTagName())
                    && "LINE".equals(el.getAttribute("type"))) {

                int missed = Integer.parseInt(el.getAttribute("missed"));
                int covered = Integer.parseInt(el.getAttribute("covered"));
                int total = missed + covered;
                double percentage = total > 0 ? (covered * 100.0) / total : 0;
                Severity severity = coverageSeverity(percentage);

                findings.add(Finding.projectLevel(
                        HeuristicCode.T1,
                        String.format("Overall line coverage: %.1f%% (%d/%d lines covered)",
                                percentage, covered, total),
                        severity, Confidence.HIGH, "jacoco", "line-coverage"));
                break;
            }
        }
    }

    private void addPerClassFindings(Document doc, List<Finding> findings) {
        NodeList classNodes = doc.getElementsByTagName("class");

        for (int i = 0; i < classNodes.getLength(); i++) {
            Element classElement = (Element) classNodes.item(i);
            String className = classElement.getAttribute("name");
            String sourceFile = classElement.getAttribute("sourcefilename");

            NodeList counters = classElement.getElementsByTagName("counter");
            for (int j = 0; j < counters.getLength(); j++) {
                Element counter = (Element) counters.item(j);
                if ("LINE".equals(counter.getAttribute("type"))) {
                    int missed = Integer.parseInt(counter.getAttribute("missed"));
                    int covered = Integer.parseInt(counter.getAttribute("covered"));
                    int total = missed + covered;

                    if (total < MIN_CLASS_LINES) {
                        break;
                    }

                    double percentage = (covered * 100.0) / total;
                    if (percentage < 50) {
                        String packagePath = className.substring(0, className.lastIndexOf('/'));
                        findings.add(Finding.at(
                                HeuristicCode.T8,
                                packagePath + "/" + sourceFile,
                                1, -1,
                                String.format("Class %s has %.1f%% line coverage (%d/%d lines)",
                                        className.substring(className.lastIndexOf('/') + 1),
                                        percentage, covered, total),
                                Severity.WARNING, Confidence.MEDIUM,
                                "jacoco", "class-coverage"));
                    }
                    break;
                }
            }
        }
    }

    private Severity coverageSeverity(double percentage) {
        if (percentage < 50) return Severity.ERROR;
        if (percentage < 75) return Severity.WARNING;
        return Severity.INFO;
    }

    private Path reportPath(ProjectContext context) {
        return context.reportsDir().resolve("jacoco/test/jacocoTestReport.xml");
    }
}
