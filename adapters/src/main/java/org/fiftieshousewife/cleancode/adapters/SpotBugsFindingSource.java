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
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SpotBugsFindingSource implements FindingSource {

    private record RuleMapping(HeuristicCode code, Severity severity, Confidence confidence, String ruleUrl) {}

    private static final String SB = "https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#";

    private static final Map<String, RuleMapping> TYPE_MAP = Map.ofEntries(
            entry("BAD_PRACTICE/BC_UNCONFIRMED_CAST",
                    HeuristicCode.G4, Severity.WARNING, Confidence.HIGH,
                    "bc-unconfirmed-cast-bc-unconfirmed-cast"),
            entry("BAD_PRACTICE/CT_CONSTRUCTOR_THROW",
                    HeuristicCode.G4, Severity.WARNING, Confidence.MEDIUM,
                    "ct-constructor-throw"),
            entry("BAD_PRACTICE/DE_MIGHT_IGNORE",
                    HeuristicCode.G4, Severity.ERROR, Confidence.HIGH,
                    "de-might-ignore"),
            entry("BAD_PRACTICE/DM_DEFAULT_ENCODING",
                    HeuristicCode.G26, Severity.WARNING, Confidence.HIGH,
                    "dm-default-encoding"),
            entry("BAD_PRACTICE/EQ_COMPARETO_USE_OBJECT_EQUALS",
                    HeuristicCode.G11, Severity.WARNING, Confidence.HIGH,
                    "eq-compareto-use-object-equals"),
            entry("BAD_PRACTICE/ES_COMPARING_STRINGS_WITH_EQ",
                    HeuristicCode.G26, Severity.WARNING, Confidence.HIGH,
                    "es-comparing-strings-with-eq"),
            entry("BAD_PRACTICE/HE_EQUALS_NO_HASHCODE",
                    HeuristicCode.G11, Severity.WARNING, Confidence.HIGH,
                    "he-equals-no-hashcode"),
            entry("BAD_PRACTICE/NP_NULL_PARAM_DEREF",
                    HeuristicCode.Ch7_2, Severity.ERROR, Confidence.HIGH,
                    "np-null-param-deref"),
            entry("BAD_PRACTICE/OS_OPEN_STREAM",
                    HeuristicCode.G4, Severity.WARNING, Confidence.HIGH,
                    "os-open-stream"),
            entry("BAD_PRACTICE/RV_RETURN_VALUE_IGNORED_BAD_PRACTICE",
                    HeuristicCode.G4, Severity.WARNING, Confidence.HIGH,
                    "rv-return-value-ignored-bad-practice"),
            entry("BAD_PRACTICE/ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
                    HeuristicCode.G18, Severity.WARNING, Confidence.HIGH,
                    "st-write-to-static-from-instance-method"),
            entry("CORRECTNESS/NP_ALWAYS_NULL",
                    HeuristicCode.Ch7_2, Severity.ERROR, Confidence.HIGH,
                    "np-always-null"),
            entry("CORRECTNESS/NP_NULL_ON_SOME_PATH",
                    HeuristicCode.Ch7_2, Severity.ERROR, Confidence.HIGH,
                    "np-null-on-some-path"),
            entry("CORRECTNESS/RE_BAD_SYNTAX_FOR_REGULAR_EXPRESSION",
                    HeuristicCode.G4, Severity.ERROR, Confidence.HIGH,
                    "re-bad-syntax-for-regular-expression"),
            entry("CORRECTNESS/RV_RETURN_VALUE_IGNORED",
                    HeuristicCode.G4, Severity.WARNING, Confidence.HIGH,
                    "rv-return-value-ignored"),
            entry("MALICIOUS_CODE/EI_EXPOSE_REP",
                    HeuristicCode.G8, Severity.WARNING, Confidence.HIGH,
                    "ei-expose-rep"),
            entry("MALICIOUS_CODE/EI_EXPOSE_REP2",
                    HeuristicCode.G8, Severity.WARNING, Confidence.HIGH,
                    "ei-expose-rep2"),
            entry("MALICIOUS_CODE/MS_MUTABLE_ARRAY",
                    HeuristicCode.G8, Severity.WARNING, Confidence.HIGH,
                    "ms-mutable-array"),
            entry("MALICIOUS_CODE/MS_MUTABLE_COLLECTION_PKGPROTECT",
                    HeuristicCode.G8, Severity.WARNING, Confidence.HIGH,
                    "ms-mutable-collection-pkgprotect"),
            entry("MALICIOUS_CODE/MS_SHOULD_BE_FINAL",
                    HeuristicCode.G22, Severity.WARNING, Confidence.HIGH,
                    "ms-should-be-final"),
            entry("PERFORMANCE/DM_BOXED_PRIMITIVE_FOR_COMPARE",
                    HeuristicCode.G26, Severity.INFO, Confidence.HIGH,
                    "dm-boxed-primitive-for-compare"),
            entry("PERFORMANCE/DM_NUMBER_CTOR",
                    HeuristicCode.G26, Severity.INFO, Confidence.HIGH,
                    "dm-number-ctor"),
            entry("PERFORMANCE/SIC_INNER_SHOULD_BE_STATIC",
                    HeuristicCode.G18, Severity.WARNING, Confidence.HIGH,
                    "sic-inner-should-be-static"),
            entry("PERFORMANCE/SS_SHOULD_BE_STATIC",
                    HeuristicCode.G18, Severity.WARNING, Confidence.MEDIUM,
                    "ss-should-be-static"),
            entry("PERFORMANCE/UUF_UNUSED_FIELD",
                    HeuristicCode.G9, Severity.INFO, Confidence.HIGH,
                    "uuf-unused-field"),
            entry("PERFORMANCE/WMI_WRONG_MAP_ITERATOR",
                    HeuristicCode.G30, Severity.INFO, Confidence.HIGH,
                    "wmi-wrong-map-iterator"),
            entry("STYLE/BC_UNCONFIRMED_CAST_OF_RETURN_VALUE",
                    HeuristicCode.G4, Severity.WARNING, Confidence.MEDIUM,
                    "bc-unconfirmed-cast-of-return-value"),
            entry("STYLE/DB_DUPLICATE_BRANCHES",
                    HeuristicCode.G5, Severity.WARNING, Confidence.HIGH,
                    "db-duplicate-branches"),
            entry("STYLE/DLS_DEAD_LOCAL_STORE",
                    HeuristicCode.G9, Severity.INFO, Confidence.HIGH,
                    "dls-dead-local-store"),
            entry("STYLE/EQ_DOESNT_OVERRIDE_EQUALS",
                    HeuristicCode.G11, Severity.WARNING, Confidence.HIGH,
                    "eq-doesnt-override-equals"),
            entry("STYLE/NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
                    HeuristicCode.Ch7_2, Severity.WARNING, Confidence.HIGH,
                    "np-null-on-some-path-from-return-value"),
            entry("STYLE/RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
                    HeuristicCode.Ch7_2, Severity.WARNING, Confidence.HIGH,
                    "rcn-redundant-nullcheck-of-nonnull-value"),
            entry("STYLE/RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
                    HeuristicCode.Ch7_2, Severity.ERROR, Confidence.HIGH,
                    "rcn-redundant-nullcheck-would-have-been-a-npe"),
            entry("STYLE/SF_SWITCH_NO_DEFAULT",
                    HeuristicCode.G23, Severity.INFO, Confidence.MEDIUM,
                    "sf-switch-no-default"),
            entry("STYLE/UC_USELESS_CONDITION",
                    HeuristicCode.G9, Severity.WARNING, Confidence.HIGH,
                    "uc-useless-condition"),
            entry("STYLE/URF_UNREAD_FIELD",
                    HeuristicCode.G9, Severity.INFO, Confidence.HIGH,
                    "urf-unread-field")
    );

    private static final Map<String, RuleMapping> CATEGORY_MAP = Map.of(
            "CORRECTNESS", new RuleMapping(HeuristicCode.G4, Severity.ERROR, Confidence.HIGH, SB + "correctness")
    );

    private static Map.Entry<String, RuleMapping> entry(final String key, final HeuristicCode code,
            final Severity severity, final Confidence confidence, final String anchor) {
        return Map.entry(key, new RuleMapping(code, severity, confidence, SB + anchor));
    }

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
        final RuleMapping mapping = mappingFor(category, type);
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

    private RuleMapping mappingFor(final String category, final String type) {
        final RuleMapping typed = TYPE_MAP.get(category + "/" + type);
        return typed != null ? typed : CATEGORY_MAP.get(category);
    }

    private String shortMessageOrType(final Element bug, final String type) {
        final NodeList shortMessages = bug.getElementsByTagName("ShortMessage");
        return shortMessages.getLength() > 0
                ? shortMessages.item(0).getTextContent().trim()
                : type;
    }

    private Path reportPath(ProjectContext context) {
        return context.reportsDir().resolve("spotbugs/main.xml");
    }
}
