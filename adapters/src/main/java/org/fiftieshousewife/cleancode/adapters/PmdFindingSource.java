package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class PmdFindingSource implements FindingSource {

    private record RuleMapping(HeuristicCode code, Severity severity, Confidence confidence) {}

    private static final Map<String, RuleMapping> RULE_MAP = Map.ofEntries(
            Map.entry("CyclomaticComplexity", new RuleMapping(HeuristicCode.G30, Severity.WARNING, Confidence.MEDIUM)),
            Map.entry("NPathComplexity", new RuleMapping(HeuristicCode.G30, Severity.WARNING, Confidence.MEDIUM)),
            Map.entry("ExcessiveMethodLength", new RuleMapping(HeuristicCode.G30, Severity.WARNING, Confidence.MEDIUM)),
            Map.entry("UnusedPrivateMethod", new RuleMapping(HeuristicCode.F4, Severity.WARNING, Confidence.HIGH)),
            Map.entry("UnusedLocalVariable", new RuleMapping(HeuristicCode.G9, Severity.INFO, Confidence.HIGH)),
            Map.entry("UnusedImports", new RuleMapping(HeuristicCode.G12, Severity.INFO, Confidence.HIGH)),
            Map.entry("ExcessivePublicCount", new RuleMapping(HeuristicCode.G8, Severity.WARNING, Confidence.HIGH)),
            Map.entry("CouplingBetweenObjects", new RuleMapping(HeuristicCode.G8, Severity.WARNING, Confidence.MEDIUM)),
            Map.entry("AvoidConstantsInterface", new RuleMapping(HeuristicCode.J2, Severity.WARNING, Confidence.HIGH)),
            Map.entry("CommentedOutCodeLine", new RuleMapping(HeuristicCode.C5, Severity.WARNING, Confidence.HIGH)),
            Map.entry("EmptyCatchBlock", new RuleMapping(HeuristicCode.G4, Severity.ERROR, Confidence.HIGH)),
            Map.entry("EmptyIfStmt", new RuleMapping(HeuristicCode.G12, Severity.WARNING, Confidence.HIGH)),
            Map.entry("SwitchStmtsShouldHaveDefault", new RuleMapping(HeuristicCode.G23, Severity.INFO, Confidence.MEDIUM)),
            Map.entry("TooManyFields", new RuleMapping(HeuristicCode.G8, Severity.WARNING, Confidence.MEDIUM)),
            Map.entry("TooManyMethods", new RuleMapping(HeuristicCode.G8, Severity.WARNING, Confidence.MEDIUM)),
            Map.entry("GodClass", new RuleMapping(HeuristicCode.G8, Severity.ERROR, Confidence.MEDIUM)),
            Map.entry("DataClass", new RuleMapping(HeuristicCode.G17, Severity.INFO, Confidence.LOW)),
            Map.entry("UseLocaleWithCaseConversions", new RuleMapping(HeuristicCode.G20, Severity.WARNING, Confidence.HIGH)),
            Map.entry("LooseCoupling", new RuleMapping(HeuristicCode.G8, Severity.WARNING, Confidence.MEDIUM)),
            Map.entry("CloseResource", new RuleMapping(HeuristicCode.G4, Severity.WARNING, Confidence.MEDIUM)),
            Map.entry("AvoidReassigningParameters", new RuleMapping(HeuristicCode.G22, Severity.WARNING, Confidence.HIGH))
    );

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

                NodeList violations = fileElement.getElementsByTagName("violation");
                for (int j = 0; j < violations.getLength(); j++) {
                    Element v = (Element) violations.item(j);
                    String rule = v.getAttribute("rule");

                    RuleMapping mapping = RULE_MAP.get(rule);
                    if (mapping == null) {
                        continue;
                    }

                    int startLine = Integer.parseInt(v.getAttribute("beginline"));
                    int endLine = Integer.parseInt(v.getAttribute("endline"));
                    String message = v.getTextContent().trim();

                    findings.add(Finding.at(
                            mapping.code(), relativePath, startLine, endLine,
                            message, mapping.severity(), mapping.confidence(),
                            "pmd", rule));
                }
            }

            return findings;
        } catch (Exception e) {
            throw new FindingSourceException("Failed to parse PMD report: " + report, e);
        }
    }

    private Path reportPath(ProjectContext context) {
        return context.reportsDir().resolve("pmd/main.xml");
    }

}
