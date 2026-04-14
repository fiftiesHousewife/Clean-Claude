package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class CpdFindingSource implements FindingSource {

    @Override
    public String id() {
        return "cpd";
    }

    @Override
    public String displayName() {
        return "CPD";
    }

    @Override
    public Set<HeuristicCode> coveredCodes() {
        return Set.of(HeuristicCode.G5);
    }

    @Override
    public boolean isAvailable(ProjectContext context) {
        return Files.exists(reportPath(context));
    }

    @Override
    public List<Finding> collectFindings(ProjectContext context) throws FindingSourceException {
        Path report = reportPath(context);
        if (!Files.exists(report)) {
            return List.of();
        }

        Document doc = XmlReportParser.parse(report);
        List<Finding> findings = new ArrayList<>();
        NodeList duplications = doc.getElementsByTagName("duplication");

        for (int i = 0; i < duplications.getLength(); i++) {
            findings.addAll(findingsForDuplication((Element) duplications.item(i), context));
        }

        return findings;
    }

    List<Finding> findingsForDuplication(Element duplication, ProjectContext context) {
        int tokens = Integer.parseInt(duplication.getAttribute("tokens"));
        int lines = Integer.parseInt(duplication.getAttribute("lines"));
        Severity severity = severityForTokenCount(tokens);
        List<Element> files = extractFileElements(duplication);

        List<Finding> findings = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            findings.add(buildFinding(files, i, tokens, lines, severity, context));
        }
        return findings;
    }

    Finding buildFinding(List<Element> files, int index, int tokens, int lines,
                         Severity severity, ProjectContext context) {
        Element file = files.get(index);
        String relativePath = PathUtils.relativise(file.getAttribute("path"), context.projectRoot());
        int startLine = Integer.parseInt(file.getAttribute("line"));
        int endLine = startLine + lines - 1;

        List<String> otherFiles = otherFilePaths(files, index, context);
        Map<String, String> metadata = Map.of(
                "otherFile", String.join(", ", otherFiles),
                "tokens", String.valueOf(tokens));

        return new Finding(
                HeuristicCode.G5, relativePath, startLine, endLine,
                "Duplicated block (" + tokens + " tokens)",
                severity, Confidence.HIGH,
                "cpd", "cpd-duplication", metadata);
    }

    List<String> otherFilePaths(List<Element> files, int excludeIndex, ProjectContext context) {
        List<String> paths = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            if (i != excludeIndex) {
                paths.add(PathUtils.relativise(files.get(i).getAttribute("path"), context.projectRoot()));
            }
        }
        return paths;
    }

    Severity severityForTokenCount(int tokens) {
        if (tokens >= 200) return Severity.ERROR;
        if (tokens >= 100) return Severity.WARNING;
        return Severity.INFO;
    }

    private List<Element> extractFileElements(Element duplication) {
        NodeList fileNodes = duplication.getElementsByTagName("file");
        List<Element> files = new ArrayList<>();
        for (int i = 0; i < fileNodes.getLength(); i++) {
            files.add((Element) fileNodes.item(i));
        }
        return files;
    }

    private Path reportPath(ProjectContext context) {
        return context.reportsDir().resolve("cpd/cpd.xml");
    }
}
