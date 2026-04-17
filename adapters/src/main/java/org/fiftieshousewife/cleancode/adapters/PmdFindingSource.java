package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.Finding;
import org.fiftieshousewife.cleancode.core.FindingSource;
import org.fiftieshousewife.cleancode.core.FindingSourceException;
import org.fiftieshousewife.cleancode.core.ProjectContext;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class PmdFindingSource implements FindingSource {

    private static final Map<String, PmdRuleMap.Entry> RULE_MAP = PmdRuleMap.load();

    @Override
    public String id() {
        return "pmd";
    }

    @Override
    public String displayName() {
        return "PMD";
    }

    @Override
    public Set<HeuristicCode> coveredCodes() {
        final Set<HeuristicCode> codes = EnumSet.noneOf(HeuristicCode.class);
        RULE_MAP.values().forEach(m -> codes.add(m.code()));
        return Collections.unmodifiableSet(codes);
    }

    @Override
    public boolean isAvailable(final ProjectContext context) {
        return Files.exists(reportPath(context));
    }

    @Override
    public List<Finding> collectFindings(final ProjectContext context) throws FindingSourceException {
        final Path report = reportPath(context);
        if (!Files.exists(report)) {
            return List.of();
        }

        try {
            final Document doc = XmlReportParser.parse(report);
            final List<Finding> findings = new ArrayList<>();
            elementsOf(doc.getElementsByTagName("file"))
                    .forEach(fileElement -> collectFromFile(fileElement, context, findings));
            return findings;
        } catch (final Exception e) {
            throw new FindingSourceException("Failed to parse PMD report: " + report, e);
        }
    }

    private void collectFromFile(final Element fileElement, final ProjectContext context,
                                 final List<Finding> findings) {
        final String absolutePath = fileElement.getAttribute("name");
        final String relativePath = PathUtils.relativise(absolutePath, context.projectRoot());
        elementsOf(fileElement.getElementsByTagName("violation"))
                .map(violation -> toFinding(violation, relativePath))
                .filter(finding -> finding != null)
                .forEach(findings::add);
    }

    private Finding toFinding(final Element violation, final String relativePath) {
        final String rule = violation.getAttribute("rule");
        final PmdRuleMap.Entry mapping = RULE_MAP.get(rule);
        if (mapping == null) {
            return null;
        }
        final int startLine = Integer.parseInt(violation.getAttribute("beginline"));
        final int endLine = Integer.parseInt(violation.getAttribute("endline"));
        final String message = violation.getTextContent().trim();
        return new Finding(
                mapping.code(), relativePath, startLine, endLine,
                message, mapping.severity(), mapping.confidence(),
                "pmd", rule, Map.of("ruleUrl", mapping.ruleUrl()));
    }

    private static Stream<Element> elementsOf(final NodeList nodes) {
        return IntStream.range(0, nodes.getLength())
                .mapToObj(nodes::item)
                .map(node -> (Element) node);
    }

    private Path reportPath(final ProjectContext context) {
        return context.reportsDir().resolve("pmd/main.xml");
    }
}
