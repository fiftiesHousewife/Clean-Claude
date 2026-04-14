package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SpotBugsFindingSource implements FindingSource {

    private record RuleMapping(HeuristicCode code, Severity severity, Confidence confidence) {}

    // Specific category/type pairs take precedence
    private static final Map<String, RuleMapping> TYPE_MAP = Map.of(
            "BAD_PRACTICE/DE_MIGHT_IGNORE", new RuleMapping(HeuristicCode.G4, Severity.ERROR, Confidence.HIGH),
            "BAD_PRACTICE/ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", new RuleMapping(HeuristicCode.G18, Severity.WARNING, Confidence.HIGH),
            "STYLE/URF_UNREAD_FIELD", new RuleMapping(HeuristicCode.G9, Severity.INFO, Confidence.HIGH),
            "PERFORMANCE/DM_BOXED_PRIMITIVE_FOR_COMPARE", new RuleMapping(HeuristicCode.G26, Severity.INFO, Confidence.HIGH),
            "MALICIOUS_CODE/MS_MUTABLE_ARRAY", new RuleMapping(HeuristicCode.G8, Severity.WARNING, Confidence.HIGH)
    );

    // Category-level fallback
    private static final Map<String, RuleMapping> CATEGORY_MAP = Map.of(
            "CORRECTNESS", new RuleMapping(HeuristicCode.G4, Severity.ERROR, Confidence.HIGH)
    );

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
        Set<HeuristicCode> codes = EnumSet.noneOf(HeuristicCode.class);
        TYPE_MAP.values().forEach(m -> codes.add(m.code()));
        CATEGORY_MAP.values().forEach(m -> codes.add(m.code()));
        return Collections.unmodifiableSet(codes);
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
        NodeList bugInstances = doc.getElementsByTagName("BugInstance");

        for (int i = 0; i < bugInstances.getLength(); i++) {
            Element bug = (Element) bugInstances.item(i);
            String type = bug.getAttribute("type");
            String category = bug.getAttribute("category");
            int rank = Integer.parseInt(bug.getAttribute("rank"));

            // Look up mapping: specific type first, then category fallback
            String typeKey = category + "/" + type;
            RuleMapping mapping = TYPE_MAP.get(typeKey);
            if (mapping == null) {
                mapping = CATEGORY_MAP.get(category);
            }
            if (mapping == null) {
                continue;
            }

            // Find SourceLine element
            NodeList sourceLines = bug.getElementsByTagName("SourceLine");
            if (sourceLines.getLength() == 0) {
                continue;
            }
            Element sourceLine = (Element) sourceLines.item(0);
            int startLine = Integer.parseInt(sourceLine.getAttribute("start"));
            int endLine = Integer.parseInt(sourceLine.getAttribute("end"));
            String sourcePath = sourceLine.getAttribute("sourcepath");

            // Get message
            NodeList shortMessages = bug.getElementsByTagName("ShortMessage");
            String message = shortMessages.getLength() > 0
                    ? shortMessages.item(0).getTextContent().trim()
                    : type;

            // Use mapping severity, with rank as fallback context
            Severity severity = mapping.severity();

            findings.add(Finding.at(
                    mapping.code(), sourcePath, startLine, endLine,
                    message, severity, mapping.confidence(),
                    "spotbugs", type));
        }

        return findings;
    }

    private Path reportPath(ProjectContext context) {
        return context.reportsDir().resolve("spotbugs/main.xml");
    }
}
