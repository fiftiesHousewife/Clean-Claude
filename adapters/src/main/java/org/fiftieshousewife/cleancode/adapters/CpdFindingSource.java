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
import java.util.Optional;
import java.util.Set;

public class CpdFindingSource implements FindingSource {

    private static final int ERROR_TOKEN_THRESHOLD = 200;

    private record DuplicationStats(int tokens, int lines, Severity severity) {}

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
        final Optional<Document> doc = loadReport(context);
        if (doc.isEmpty()) {
            return List.of();
        }

        final List<Finding> findings = new ArrayList<>();
        final NodeList duplications = doc.get().getElementsByTagName("duplication");

        for (int i = 0; i < duplications.getLength(); i++) {
            findings.addAll(findingsForDuplication((Element) duplications.item(i), context));
        }

        return findings;
    }

    List<Finding> findingsForDuplication(final Element duplication, final ProjectContext context) {
        final int tokens = Integer.parseInt(duplication.getAttribute("tokens"));
        final int lines = Integer.parseInt(duplication.getAttribute("lines"));
        final DuplicationStats stats = new DuplicationStats(tokens, lines, severityForTokenCount(tokens));
        final List<Element> files = extractFileElements(duplication);

        final List<Finding> findings = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            findings.add(buildFinding(files, i, stats, context));
        }
        return findings;
    }

    Finding buildFinding(final List<Element> files, final int index,
                         final DuplicationStats stats, final ProjectContext context) {
        final Element file = files.get(index);
        final String relativePath = PathUtils.relativise(file.getAttribute("path"), context.projectRoot());
        final int startLine = Integer.parseInt(file.getAttribute("line"));
        final int endLine = startLine + stats.lines() - 1;

        final List<String> otherFiles = otherFilePaths(files, index, context);
        final Map<String, String> metadata = Map.of(
                "otherFile", String.join(", ", otherFiles),
                "tokens", String.valueOf(stats.tokens()));

        return new Finding(
                HeuristicCode.G5, relativePath, startLine, endLine,
                "Duplicated block (" + stats.tokens() + " tokens)",
                stats.severity(), Confidence.HIGH,
                "cpd", "cpd-duplication", metadata);
    }

    List<String> otherFilePaths(final List<Element> files, final int excludeIndex, final ProjectContext context) {
        final List<String> paths = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            if (i != excludeIndex) {
                paths.add(PathUtils.relativise(files.get(i).getAttribute("path"), context.projectRoot()));
            }
        }
        return paths;
    }

    Severity severityForTokenCount(final int tokens) {
        if (tokens >= ERROR_TOKEN_THRESHOLD) {
            return Severity.ERROR;
        }
        return Severity.WARNING;
    }

    private Optional<Document> loadReport(final ProjectContext context) throws FindingSourceException {
        final Path report = reportPath(context);
        if (!Files.exists(report)) {
            return Optional.empty();
        }
        return Optional.of(XmlReportParser.parse(report));
    }

    private List<Element> extractFileElements(final Element duplication) {
        final NodeList fileNodes = duplication.getElementsByTagName("file");
        final List<Element> files = new ArrayList<>();
        for (int i = 0; i < fileNodes.getLength(); i++) {
            files.add((Element) fileNodes.item(i));
        }
        return files;
    }

    private Path reportPath(final ProjectContext context) {
        return context.reportsDir().resolve("cpd/cpd.xml");
    }
}
