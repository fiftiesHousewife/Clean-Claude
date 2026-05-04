package io.github.fiftieshousewife.cleancode.adapters;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;
import io.github.fiftieshousewife.cleancode.core.Confidence;
import io.github.fiftieshousewife.cleancode.core.Finding;
import io.github.fiftieshousewife.cleancode.core.FindingSource;
import io.github.fiftieshousewife.cleancode.core.FindingSourceException;
import io.github.fiftieshousewife.cleancode.core.ProjectContext;
import io.github.fiftieshousewife.cleancode.core.Severity;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public boolean isAvailable(final ProjectContext context) {
        return Files.exists(reportPath(context));
    }

    @Override
    public List<Finding> collectFindings(final ProjectContext context) throws FindingSourceException {
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

    List<Finding> findingsForDuplication(final Element duplication, final ProjectContext context) {
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

    Finding buildFinding(final List<Element> files, final int index, final int tokens, final int lines,
                         final Severity severity, final ProjectContext context) {
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

    List<String> otherFilePaths(final List<Element> files, final int excludeIndex, final ProjectContext context) {
        List<String> paths = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            if (i != excludeIndex) {
                paths.add(PathUtils.relativise(files.get(i).getAttribute("path"), context.projectRoot()));
            }
        }
        return paths;
    }

    Severity severityForTokenCount(final int tokens) {
        if (tokens >= 200) {
            return Severity.ERROR;
        }
        return Severity.WARNING;
    }

    private List<Element> extractFileElements(final Element duplication) {
        NodeList fileNodes = duplication.getElementsByTagName("file");
        List<Element> files = new ArrayList<>();
        for (int i = 0; i < fileNodes.getLength(); i++) {
            files.add((Element) fileNodes.item(i));
        }
        return files;
    }

    private Path reportPath(final ProjectContext context) {
        return context.reportsDir().resolve("cpd/cpd.xml");
    }
}
