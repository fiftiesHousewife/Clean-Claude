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
import java.util.Map;
import java.util.Set;

// TODO: G5 — requires human review: the coveredCodes/isAvailable/collectFindings preamble
// is duplicated across every *FindingSource in this package. Extracting it affects the
// FindingSource contract and every sibling implementation, which is out of scope for a
// single-file fix.
public class CpdFindingSource implements FindingSource {

    private record Duplication(List<Element> files, int tokens, int lines, Severity severity) {}

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
        final Path report = reportPath(context);
        if (!Files.exists(report)) {
            return List.of();
        }

        final Document doc = XmlReportParser.parse(report);
        final List<Finding> findings = new ArrayList<>();
        final NodeList duplications = doc.getElementsByTagName("duplication");

        for (int i = 0; i < duplications.getLength(); i++) {
            findings.addAll(findingsForDuplication((Element) duplications.item(i), context));
        }

        return findings;
    }

    List<Finding> findingsForDuplication(Element duplicationElement, ProjectContext context) {
        final Duplication duplication = parseDuplication(duplicationElement);
        final List<Finding> findings = new ArrayList<>();
        for (int i = 0; i < duplication.files().size(); i++) {
            findings.add(buildFinding(duplication, i, context));
        }
        return findings;
    }

    Finding buildFinding(Duplication duplication, int index, ProjectContext context) {
        final Element file = duplication.files().get(index);
        final String relativePath = RelativePath.of(file.getAttribute("path"), context.projectRoot());
        final int startLine = Integer.parseInt(file.getAttribute("line"));
        final int endLine = startLine + duplication.lines() - 1;

        final List<String> otherFiles = otherFilePaths(duplication.files(), index, context);
        final Map<String, String> metadata = Map.of(
                "otherFile", String.join(", ", otherFiles),
                "tokens", String.valueOf(duplication.tokens()));

        return new Finding(
                HeuristicCode.G5, relativePath, startLine, endLine,
                "Duplicated block (" + duplication.tokens() + " tokens)",
                duplication.severity(), Confidence.HIGH,
                "cpd", "cpd-duplication", metadata);
    }

    List<String> otherFilePaths(List<Element> files, int excludeIndex, ProjectContext context) {
        final List<String> paths = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            if (i != excludeIndex) {
                paths.add(RelativePath.of(files.get(i).getAttribute("path"), context.projectRoot()));
            }
        }
        return paths;
    }

    Severity severityForTokenCount(int tokens) {
        if (tokens >= 200) {
            return Severity.ERROR;
        }
        return Severity.WARNING;
    }

    private Duplication parseDuplication(Element duplicationElement) {
        final int tokens = Integer.parseInt(duplicationElement.getAttribute("tokens"));
        final int lines = Integer.parseInt(duplicationElement.getAttribute("lines"));
        final Severity severity = severityForTokenCount(tokens);
        final List<Element> files = extractFileElements(duplicationElement);
        return new Duplication(files, tokens, lines, severity);
    }

    private List<Element> extractFileElements(Element duplicationElement) {
        final NodeList fileNodes = duplicationElement.getElementsByTagName("file");
        final List<Element> files = new ArrayList<>();
        for (int i = 0; i < fileNodes.getLength(); i++) {
            files.add((Element) fileNodes.item(i));
        }
        return files;
    }

    private Path reportPath(ProjectContext context) {
        return context.reportsDir().resolve("cpd/cpd.xml");
    }
}
