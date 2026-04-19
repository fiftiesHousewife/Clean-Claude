package io.github.fiftieshousewife.cleancode.adapters;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;
import io.github.fiftieshousewife.cleancode.core.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SpotBugsFindingSource implements FindingSource {

    private record RuleMapping(HeuristicCode code, Severity severity, Confidence confidence, String ruleUrl) {}

    private static final String SB = "https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#";

    private static final Map<String, RuleMapping> TYPE_MAP = Map.ofEntries(
            Map.entry("BAD_PRACTICE/BC_UNCONFIRMED_CAST", new RuleMapping(HeuristicCode.G4, Severity.WARNING, Confidence.HIGH, SB + "bc-unconfirmed-cast-bc-unconfirmed-cast")),
            Map.entry("BAD_PRACTICE/CT_CONSTRUCTOR_THROW", new RuleMapping(HeuristicCode.G4, Severity.WARNING, Confidence.MEDIUM, SB + "ct-constructor-throw")),
            Map.entry("BAD_PRACTICE/DE_MIGHT_IGNORE", new RuleMapping(HeuristicCode.G4, Severity.ERROR, Confidence.HIGH, SB + "de-might-ignore")),
            Map.entry("BAD_PRACTICE/DM_DEFAULT_ENCODING", new RuleMapping(HeuristicCode.G26, Severity.WARNING, Confidence.HIGH, SB + "dm-default-encoding")),
            Map.entry("BAD_PRACTICE/EQ_COMPARETO_USE_OBJECT_EQUALS", new RuleMapping(HeuristicCode.G11, Severity.WARNING, Confidence.HIGH, SB + "eq-compareto-use-object-equals")),
            Map.entry("BAD_PRACTICE/ES_COMPARING_STRINGS_WITH_EQ", new RuleMapping(HeuristicCode.G26, Severity.WARNING, Confidence.HIGH, SB + "es-comparing-strings-with-eq")),
            Map.entry("BAD_PRACTICE/HE_EQUALS_NO_HASHCODE", new RuleMapping(HeuristicCode.G11, Severity.WARNING, Confidence.HIGH, SB + "he-equals-no-hashcode")),
            Map.entry("BAD_PRACTICE/NP_NULL_PARAM_DEREF", new RuleMapping(HeuristicCode.Ch7_2, Severity.ERROR, Confidence.HIGH, SB + "np-null-param-deref")),
            Map.entry("BAD_PRACTICE/OS_OPEN_STREAM", new RuleMapping(HeuristicCode.G4, Severity.WARNING, Confidence.HIGH, SB + "os-open-stream")),
            Map.entry("BAD_PRACTICE/RV_RETURN_VALUE_IGNORED_BAD_PRACTICE", new RuleMapping(HeuristicCode.G4, Severity.WARNING, Confidence.HIGH, SB + "rv-return-value-ignored-bad-practice")),
            Map.entry("BAD_PRACTICE/ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", new RuleMapping(HeuristicCode.G18, Severity.WARNING, Confidence.HIGH, SB + "st-write-to-static-from-instance-method")),
            Map.entry("CORRECTNESS/NP_ALWAYS_NULL", new RuleMapping(HeuristicCode.Ch7_2, Severity.ERROR, Confidence.HIGH, SB + "np-always-null")),
            Map.entry("CORRECTNESS/NP_NULL_ON_SOME_PATH", new RuleMapping(HeuristicCode.Ch7_2, Severity.ERROR, Confidence.HIGH, SB + "np-null-on-some-path")),
            Map.entry("CORRECTNESS/RE_BAD_SYNTAX_FOR_REGULAR_EXPRESSION", new RuleMapping(HeuristicCode.G4, Severity.ERROR, Confidence.HIGH, SB + "re-bad-syntax-for-regular-expression")),
            Map.entry("CORRECTNESS/RV_RETURN_VALUE_IGNORED", new RuleMapping(HeuristicCode.G4, Severity.WARNING, Confidence.HIGH, SB + "rv-return-value-ignored")),
            Map.entry("MALICIOUS_CODE/EI_EXPOSE_REP", new RuleMapping(HeuristicCode.G8, Severity.WARNING, Confidence.HIGH, SB + "ei-expose-rep")),
            Map.entry("MALICIOUS_CODE/EI_EXPOSE_REP2", new RuleMapping(HeuristicCode.G8, Severity.WARNING, Confidence.HIGH, SB + "ei-expose-rep2")),
            Map.entry("MALICIOUS_CODE/MS_MUTABLE_ARRAY", new RuleMapping(HeuristicCode.G8, Severity.WARNING, Confidence.HIGH, SB + "ms-mutable-array")),
            Map.entry("MALICIOUS_CODE/MS_MUTABLE_COLLECTION_PKGPROTECT", new RuleMapping(HeuristicCode.G8, Severity.WARNING, Confidence.HIGH, SB + "ms-mutable-collection-pkgprotect")),
            Map.entry("MALICIOUS_CODE/MS_SHOULD_BE_FINAL", new RuleMapping(HeuristicCode.G22, Severity.WARNING, Confidence.HIGH, SB + "ms-should-be-final")),
            Map.entry("PERFORMANCE/DM_BOXED_PRIMITIVE_FOR_COMPARE", new RuleMapping(HeuristicCode.G26, Severity.INFO, Confidence.HIGH, SB + "dm-boxed-primitive-for-compare")),
            Map.entry("PERFORMANCE/DM_NUMBER_CTOR", new RuleMapping(HeuristicCode.G26, Severity.INFO, Confidence.HIGH, SB + "dm-number-ctor")),
            Map.entry("PERFORMANCE/SIC_INNER_SHOULD_BE_STATIC", new RuleMapping(HeuristicCode.G18, Severity.WARNING, Confidence.HIGH, SB + "sic-inner-should-be-static")),
            Map.entry("PERFORMANCE/SS_SHOULD_BE_STATIC", new RuleMapping(HeuristicCode.G18, Severity.WARNING, Confidence.MEDIUM, SB + "ss-should-be-static")),
            Map.entry("PERFORMANCE/UUF_UNUSED_FIELD", new RuleMapping(HeuristicCode.G9, Severity.INFO, Confidence.HIGH, SB + "uuf-unused-field")),
            Map.entry("PERFORMANCE/WMI_WRONG_MAP_ITERATOR", new RuleMapping(HeuristicCode.G30, Severity.INFO, Confidence.HIGH, SB + "wmi-wrong-map-iterator")),
            Map.entry("STYLE/BC_UNCONFIRMED_CAST_OF_RETURN_VALUE", new RuleMapping(HeuristicCode.G4, Severity.WARNING, Confidence.MEDIUM, SB + "bc-unconfirmed-cast-of-return-value")),
            Map.entry("STYLE/DB_DUPLICATE_BRANCHES", new RuleMapping(HeuristicCode.G5, Severity.WARNING, Confidence.HIGH, SB + "db-duplicate-branches")),
            Map.entry("STYLE/DLS_DEAD_LOCAL_STORE", new RuleMapping(HeuristicCode.G9, Severity.INFO, Confidence.HIGH, SB + "dls-dead-local-store")),
            Map.entry("STYLE/EQ_DOESNT_OVERRIDE_EQUALS", new RuleMapping(HeuristicCode.G11, Severity.WARNING, Confidence.HIGH, SB + "eq-doesnt-override-equals")),
            Map.entry("STYLE/NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", new RuleMapping(HeuristicCode.Ch7_2, Severity.WARNING, Confidence.HIGH, SB + "np-null-on-some-path-from-return-value")),
            Map.entry("STYLE/RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", new RuleMapping(HeuristicCode.Ch7_2, Severity.WARNING, Confidence.HIGH, SB + "rcn-redundant-nullcheck-of-nonnull-value")),
            Map.entry("STYLE/RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", new RuleMapping(HeuristicCode.Ch7_2, Severity.ERROR, Confidence.HIGH, SB + "rcn-redundant-nullcheck-would-have-been-a-npe")),
            Map.entry("STYLE/SF_SWITCH_NO_DEFAULT", new RuleMapping(HeuristicCode.G23, Severity.INFO, Confidence.MEDIUM, SB + "sf-switch-no-default")),
            Map.entry("STYLE/UC_USELESS_CONDITION", new RuleMapping(HeuristicCode.G9, Severity.WARNING, Confidence.HIGH, SB + "uc-useless-condition")),
            Map.entry("STYLE/URF_UNREAD_FIELD", new RuleMapping(HeuristicCode.G9, Severity.INFO, Confidence.HIGH, SB + "urf-unread-field"))
    );

    private static final Map<String, RuleMapping> CATEGORY_MAP = Map.of(
            "CORRECTNESS", new RuleMapping(HeuristicCode.G4, Severity.ERROR, Confidence.HIGH, SB + "correctness")
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

            findings.add(new Finding(
                    mapping.code(), sourcePath, startLine, endLine,
                    message, severity, mapping.confidence(),
                    "spotbugs", type, Map.of("ruleUrl", mapping.ruleUrl())));
        }

        return findings;
    }

    private Path reportPath(ProjectContext context) {
        return context.reportsDir().resolve("spotbugs/main.xml");
    }
}
