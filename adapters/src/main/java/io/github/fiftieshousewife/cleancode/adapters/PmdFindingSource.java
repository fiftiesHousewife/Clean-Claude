package io.github.fiftieshousewife.cleancode.adapters;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;
import io.github.fiftieshousewife.cleancode.core.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class PmdFindingSource implements FindingSource {

    private record RuleMapping(HeuristicCode code, Severity severity, Confidence confidence, String ruleUrl) {}

    private static final String PMD_BP = "https://pmd.github.io/pmd/pmd_rules_java_bestpractices.html#";
    private static final String PMD_DS = "https://pmd.github.io/pmd/pmd_rules_java_design.html#";
    private static final String PMD_EP = "https://pmd.github.io/pmd/pmd_rules_java_errorprone.html#";
    private static final String PMD_CS = "https://pmd.github.io/pmd/pmd_rules_java_codestyle.html#";

    private static final Map<String, RuleMapping> RULE_MAP = Map.ofEntries(
            Map.entry("CyclomaticComplexity", new RuleMapping(HeuristicCode.G30, Severity.WARNING, Confidence.MEDIUM, PMD_DS + "cyclomaticcomplexity")),
            Map.entry("NPathComplexity", new RuleMapping(HeuristicCode.G30, Severity.WARNING, Confidence.MEDIUM, PMD_DS + "npathcomplexity")),
            Map.entry("ExcessiveMethodLength", new RuleMapping(HeuristicCode.G30, Severity.WARNING, Confidence.MEDIUM, PMD_DS + "excessivemethodlength")),
            Map.entry("UnusedPrivateMethod", new RuleMapping(HeuristicCode.F4, Severity.WARNING, Confidence.HIGH, PMD_BP + "unusedprivatemethod")),
            Map.entry("UnusedLocalVariable", new RuleMapping(HeuristicCode.G9, Severity.INFO, Confidence.HIGH, PMD_BP + "unusedlocalvariable")),
            Map.entry("UnusedImports", new RuleMapping(HeuristicCode.G12, Severity.INFO, Confidence.HIGH, PMD_BP + "unusedimports")),
            Map.entry("ExcessivePublicCount", new RuleMapping(HeuristicCode.G8, Severity.WARNING, Confidence.HIGH, PMD_DS + "excessivepubliccount")),
            Map.entry("CouplingBetweenObjects", new RuleMapping(HeuristicCode.G8, Severity.WARNING, Confidence.MEDIUM, PMD_DS + "couplingbetweenobjects")),
            Map.entry("AvoidConstantsInterface", new RuleMapping(HeuristicCode.J2, Severity.WARNING, Confidence.HIGH, PMD_BP + "avoidconstantsinterface")),
            Map.entry("CommentedOutCodeLine", new RuleMapping(HeuristicCode.C5, Severity.WARNING, Confidence.HIGH, PMD_CS + "commentedoutcodeline")),
            Map.entry("EmptyCatchBlock", new RuleMapping(HeuristicCode.G4, Severity.ERROR, Confidence.HIGH, PMD_EP + "emptycatchblock")),
            Map.entry("EmptyIfStmt", new RuleMapping(HeuristicCode.G12, Severity.WARNING, Confidence.HIGH, PMD_EP + "emptyifstmt")),
            Map.entry("SwitchStmtsShouldHaveDefault", new RuleMapping(HeuristicCode.G23, Severity.INFO, Confidence.MEDIUM, PMD_BP + "switchstmtsshouldhavedefault")),
            Map.entry("TooManyFields", new RuleMapping(HeuristicCode.G8, Severity.WARNING, Confidence.MEDIUM, PMD_DS + "toomanyfields")),
            Map.entry("TooManyMethods", new RuleMapping(HeuristicCode.G8, Severity.WARNING, Confidence.MEDIUM, PMD_DS + "toomanymethods")),
            Map.entry("GodClass", new RuleMapping(HeuristicCode.G8, Severity.ERROR, Confidence.MEDIUM, PMD_DS + "godclass")),
            Map.entry("DataClass", new RuleMapping(HeuristicCode.G17, Severity.INFO, Confidence.LOW, PMD_DS + "dataclass")),
            Map.entry("UseLocaleWithCaseConversions", new RuleMapping(HeuristicCode.G26, Severity.WARNING, Confidence.HIGH, PMD_EP + "uselocalewithcaseconversions")),
            Map.entry("LooseCoupling", new RuleMapping(HeuristicCode.G8, Severity.WARNING, Confidence.MEDIUM, PMD_DS + "loosecoupling")),
            Map.entry("CloseResource", new RuleMapping(HeuristicCode.G4, Severity.WARNING, Confidence.MEDIUM, PMD_EP + "closeresource")),
            Map.entry("AvoidReassigningParameters", new RuleMapping(HeuristicCode.G22, Severity.WARNING, Confidence.HIGH, PMD_BP + "avoidreassigningparameters"))
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

                    findings.add(new Finding(
                            mapping.code(), relativePath, startLine, endLine,
                            message, mapping.severity(), mapping.confidence(),
                            "pmd", rule, Map.of("ruleUrl", mapping.ruleUrl())));
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
