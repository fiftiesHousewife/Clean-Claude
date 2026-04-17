package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.adapters.SpotBugsRuleMap.RuleMapping;
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
import java.util.Optional;
import java.util.Set;

public class SpotBugsFindingSource implements FindingSource {

    @Override
    public String id() {
        return "spotbugs";
    }

    @Override
    public String displayName() {
        return "SpotBugs";
    }

    @Override
    public Set<HeuristicCode> coveredCodes() {
        final Set<HeuristicCode> codes = EnumSet.noneOf(HeuristicCode.class);
        SpotBugsRuleMap.all().forEach(m -> codes.add(m.code()));
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

        final Document doc = XmlReportParser.parse(report);
        final NodeList bugInstances = doc.getElementsByTagName("BugInstance");
        final List<Finding> findings = new ArrayList<>();
        for (int i = 0; i < bugInstances.getLength(); i++) {
            toFinding((Element) bugInstances.item(i)).ifPresent(findings::add);
        }
        return findings;
    }

    private Optional<Finding> toFinding(final Element bug) {
        final String type = bug.getAttribute("type");
        final String category = bug.getAttribute("category");
        final RuleMapping mapping = SpotBugsRuleMap.lookup(category, type);
        if (mapping == null) {
            return Optional.empty();
        }
        final NodeList sourceLines = bug.getElementsByTagName("SourceLine");
        if (sourceLines.getLength() == 0) {
            return Optional.empty();
        }
        final Element sourceLine = (Element) sourceLines.item(0);
        final int startLine = Integer.parseInt(sourceLine.getAttribute("start"));
        final int endLine = Integer.parseInt(sourceLine.getAttribute("end"));
        final String sourcePath = sourceLine.getAttribute("sourcepath");
        return Optional.of(new Finding(
                mapping.code(), sourcePath, startLine, endLine,
                shortMessageOrType(bug, type), mapping.severity(), mapping.confidence(),
                "spotbugs", type, Map.of("ruleUrl", mapping.ruleUrl())));
    }

    private String shortMessageOrType(final Element bug, final String type) {
        final NodeList shortMessages = bug.getElementsByTagName("ShortMessage");
        return shortMessages.getLength() > 0
                ? shortMessages.item(0).getTextContent().trim()
                : type;
    }

    private Path reportPath(final ProjectContext context) {
        return context.reportsDir().resolve("spotbugs/main.xml");
    }
}
