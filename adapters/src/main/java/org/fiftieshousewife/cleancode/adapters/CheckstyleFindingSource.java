package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class CheckstyleFindingSource implements FindingSource {

    private record RuleMapping(HeuristicCode code, Severity severity, Confidence confidence) {}

    private static final Map<String, RuleMapping> RULE_MAP = Map.ofEntries(
            Map.entry("ParameterNumber", new RuleMapping(HeuristicCode.F1, Severity.WARNING, Confidence.HIGH)),
            Map.entry("MagicNumber", new RuleMapping(HeuristicCode.G25, Severity.WARNING, Confidence.HIGH)),
            Map.entry("LocalVariableName", new RuleMapping(HeuristicCode.N1, Severity.WARNING, Confidence.MEDIUM)),
            Map.entry("MethodLength", new RuleMapping(HeuristicCode.G30, Severity.WARNING, Confidence.MEDIUM)),
            Map.entry("AnonInnerLength", new RuleMapping(HeuristicCode.G30, Severity.WARNING, Confidence.MEDIUM)),
            Map.entry("AvoidStarImport", new RuleMapping(HeuristicCode.J1, Severity.WARNING, Confidence.HIGH)),
            Map.entry("IllegalImport", new RuleMapping(HeuristicCode.G12, Severity.WARNING, Confidence.HIGH)),
            Map.entry("InterfaceIsType", new RuleMapping(HeuristicCode.J2, Severity.WARNING, Confidence.HIGH)),
            Map.entry("VisibilityModifier", new RuleMapping(HeuristicCode.G8, Severity.WARNING, Confidence.MEDIUM)),
            Map.entry("HideUtilityClassConstructor", new RuleMapping(HeuristicCode.G18, Severity.WARNING, Confidence.HIGH)),
            Map.entry("OneTopLevelClass", new RuleMapping(HeuristicCode.G12, Severity.WARNING, Confidence.HIGH)),
            Map.entry("NeedBraces", new RuleMapping(HeuristicCode.G28, Severity.WARNING, Confidence.MEDIUM)),
            Map.entry("LeftCurly", new RuleMapping(HeuristicCode.G24, Severity.WARNING, Confidence.HIGH)),
            Map.entry("RightCurly", new RuleMapping(HeuristicCode.G24, Severity.WARNING, Confidence.HIGH)),
            Map.entry("WhitespaceAround", new RuleMapping(HeuristicCode.G24, Severity.WARNING, Confidence.HIGH)),
            Map.entry("EmptyLineSeparator", new RuleMapping(HeuristicCode.G10, Severity.WARNING, Confidence.MEDIUM)),
            Map.entry("MethodName", new RuleMapping(HeuristicCode.N1, Severity.WARNING, Confidence.MEDIUM)),
            Map.entry("TypeName", new RuleMapping(HeuristicCode.N1, Severity.WARNING, Confidence.MEDIUM))
    );

    @Override
    public String id() {
        return "checkstyle";
    }

    @Override
    public String displayName() {
        return "Checkstyle";
    }

    @Override
    public Set<HeuristicCode> coveredCodes() {
        Set<HeuristicCode> codes = EnumSet.noneOf(HeuristicCode.class);
        RULE_MAP.values().forEach(m -> codes.add(m.code()));
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

        try {
            Document doc = XmlReportParser.parse(report);

            List<Finding> findings = new ArrayList<>();
            NodeList fileNodes = doc.getElementsByTagName("file");

            for (int i = 0; i < fileNodes.getLength(); i++) {
                Element fileElement = (Element) fileNodes.item(i);
                String absolutePath = fileElement.getAttribute("name");
                String relativePath = PathUtils.relativise(absolutePath, context.projectRoot());

                NodeList errors = fileElement.getElementsByTagName("error");
                for (int j = 0; j < errors.getLength(); j++) {
                    Element e = (Element) errors.item(j);
                    String sourceFqn = e.getAttribute("source");
                    String checkName = extractCheckName(sourceFqn);

                    RuleMapping mapping = RULE_MAP.get(checkName);
                    if (mapping == null) {
                        continue;
                    }

                    int line = Integer.parseInt(e.getAttribute("line"));
                    String message = e.getAttribute("message");
                    Severity severity = xmlSeverityOrDefault(e.getAttribute("severity"), mapping.severity());

                    findings.add(Finding.at(
                            mapping.code(), relativePath, line, line,
                            message, severity, mapping.confidence(),
                            "checkstyle", checkName));
                }
            }

            return findings;
        } catch (Exception e) {
            throw new FindingSourceException("Failed to parse Checkstyle report: " + report, e);
        }
    }

    private Path reportPath(ProjectContext context) {
        return context.reportsDir().resolve("checkstyle/main.xml");
    }

    private String extractCheckName(String sourceFqn) {
        int lastDot = sourceFqn.lastIndexOf('.');
        String simpleName = lastDot >= 0 ? sourceFqn.substring(lastDot + 1) : sourceFqn;
        if (simpleName.endsWith("Check")) {
            simpleName = simpleName.substring(0, simpleName.length() - "Check".length());
        }
        return simpleName;
    }

    private Severity xmlSeverityOrDefault(String xmlSeverity, Severity defaultSeverity) {
        return switch (xmlSeverity) {
            case "error" -> Severity.ERROR;
            case "info" -> Severity.INFO;
            case "warning" -> Severity.WARNING;
            default -> defaultSeverity;
        };
    }
}
