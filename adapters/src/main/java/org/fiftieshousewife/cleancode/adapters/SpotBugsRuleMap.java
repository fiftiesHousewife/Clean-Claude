package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.Confidence;
import org.fiftieshousewife.cleancode.core.Severity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

final class SpotBugsRuleMap {

    record RuleMapping(HeuristicCode code, Severity severity, Confidence confidence, String ruleUrl) {}

    private static final String SPOTBUGS_DOCS = "https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#";

    private static final Map<String, RuleMapping> BY_CATEGORY_AND_TYPE = Map.ofEntries(
            warning("BAD_PRACTICE/BC_UNCONFIRMED_CAST", HeuristicCode.G4, Confidence.HIGH,
                    "bc-unconfirmed-cast-bc-unconfirmed-cast"),
            warning("BAD_PRACTICE/CT_CONSTRUCTOR_THROW", HeuristicCode.G4, Confidence.MEDIUM,
                    "ct-constructor-throw"),
            error("BAD_PRACTICE/DE_MIGHT_IGNORE", HeuristicCode.G4, "de-might-ignore"),
            warning("BAD_PRACTICE/DM_DEFAULT_ENCODING", HeuristicCode.G26, Confidence.HIGH,
                    "dm-default-encoding"),
            warning("BAD_PRACTICE/EQ_COMPARETO_USE_OBJECT_EQUALS", HeuristicCode.G11, Confidence.HIGH,
                    "eq-compareto-use-object-equals"),
            warning("BAD_PRACTICE/ES_COMPARING_STRINGS_WITH_EQ", HeuristicCode.G26, Confidence.HIGH,
                    "es-comparing-strings-with-eq"),
            warning("BAD_PRACTICE/HE_EQUALS_NO_HASHCODE", HeuristicCode.G11, Confidence.HIGH,
                    "he-equals-no-hashcode"),
            error("BAD_PRACTICE/NP_NULL_PARAM_DEREF", HeuristicCode.Ch7_2, "np-null-param-deref"),
            warning("BAD_PRACTICE/OS_OPEN_STREAM", HeuristicCode.G4, Confidence.HIGH, "os-open-stream"),
            warning("BAD_PRACTICE/RV_RETURN_VALUE_IGNORED_BAD_PRACTICE", HeuristicCode.G4, Confidence.HIGH,
                    "rv-return-value-ignored-bad-practice"),
            warning("BAD_PRACTICE/ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", HeuristicCode.G18, Confidence.HIGH,
                    "st-write-to-static-from-instance-method"),
            error("CORRECTNESS/NP_ALWAYS_NULL", HeuristicCode.Ch7_2, "np-always-null"),
            error("CORRECTNESS/NP_NULL_ON_SOME_PATH", HeuristicCode.Ch7_2, "np-null-on-some-path"),
            error("CORRECTNESS/RE_BAD_SYNTAX_FOR_REGULAR_EXPRESSION", HeuristicCode.G4,
                    "re-bad-syntax-for-regular-expression"),
            warning("CORRECTNESS/RV_RETURN_VALUE_IGNORED", HeuristicCode.G4, Confidence.HIGH,
                    "rv-return-value-ignored"),
            warning("MALICIOUS_CODE/EI_EXPOSE_REP", HeuristicCode.G8, Confidence.HIGH, "ei-expose-rep"),
            warning("MALICIOUS_CODE/EI_EXPOSE_REP2", HeuristicCode.G8, Confidence.HIGH, "ei-expose-rep2"),
            warning("MALICIOUS_CODE/MS_MUTABLE_ARRAY", HeuristicCode.G8, Confidence.HIGH, "ms-mutable-array"),
            warning("MALICIOUS_CODE/MS_MUTABLE_COLLECTION_PKGPROTECT", HeuristicCode.G8, Confidence.HIGH,
                    "ms-mutable-collection-pkgprotect"),
            warning("MALICIOUS_CODE/MS_SHOULD_BE_FINAL", HeuristicCode.G22, Confidence.HIGH,
                    "ms-should-be-final"),
            info("PERFORMANCE/DM_BOXED_PRIMITIVE_FOR_COMPARE", HeuristicCode.G26,
                    "dm-boxed-primitive-for-compare"),
            info("PERFORMANCE/DM_NUMBER_CTOR", HeuristicCode.G26, "dm-number-ctor"),
            warning("PERFORMANCE/SIC_INNER_SHOULD_BE_STATIC", HeuristicCode.G18, Confidence.HIGH,
                    "sic-inner-should-be-static"),
            warning("PERFORMANCE/SS_SHOULD_BE_STATIC", HeuristicCode.G18, Confidence.MEDIUM,
                    "ss-should-be-static"),
            info("PERFORMANCE/UUF_UNUSED_FIELD", HeuristicCode.G9, "uuf-unused-field"),
            info("PERFORMANCE/WMI_WRONG_MAP_ITERATOR", HeuristicCode.G30, "wmi-wrong-map-iterator"),
            warning("STYLE/BC_UNCONFIRMED_CAST_OF_RETURN_VALUE", HeuristicCode.G4, Confidence.MEDIUM,
                    "bc-unconfirmed-cast-of-return-value"),
            warning("STYLE/DB_DUPLICATE_BRANCHES", HeuristicCode.G5, Confidence.HIGH, "db-duplicate-branches"),
            info("STYLE/DLS_DEAD_LOCAL_STORE", HeuristicCode.G9, "dls-dead-local-store"),
            warning("STYLE/EQ_DOESNT_OVERRIDE_EQUALS", HeuristicCode.G11, Confidence.HIGH,
                    "eq-doesnt-override-equals"),
            warning("STYLE/NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", HeuristicCode.Ch7_2, Confidence.HIGH,
                    "np-null-on-some-path-from-return-value"),
            warning("STYLE/RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", HeuristicCode.Ch7_2, Confidence.HIGH,
                    "rcn-redundant-nullcheck-of-nonnull-value"),
            error("STYLE/RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", HeuristicCode.Ch7_2,
                    "rcn-redundant-nullcheck-would-have-been-a-npe"),
            info("STYLE/SF_SWITCH_NO_DEFAULT", HeuristicCode.G23, Confidence.MEDIUM, "sf-switch-no-default"),
            warning("STYLE/UC_USELESS_CONDITION", HeuristicCode.G9, Confidence.HIGH, "uc-useless-condition"),
            info("STYLE/URF_UNREAD_FIELD", HeuristicCode.G9, "urf-unread-field")
    );

    private static final Map<String, RuleMapping> BY_CATEGORY = Map.of(
            "CORRECTNESS", new RuleMapping(HeuristicCode.G4, Severity.ERROR, Confidence.HIGH,
                    SPOTBUGS_DOCS + "correctness")
    );

    private SpotBugsRuleMap() {}

    static RuleMapping lookup(final String category, final String type) {
        final RuleMapping typed = BY_CATEGORY_AND_TYPE.get(category + "/" + type);
        return typed != null ? typed : BY_CATEGORY.get(category);
    }

    static Collection<RuleMapping> all() {
        final List<RuleMapping> mappings = new ArrayList<>(BY_CATEGORY_AND_TYPE.values());
        mappings.addAll(BY_CATEGORY.values());
        return mappings;
    }

    private static Map.Entry<String, RuleMapping> warning(
            final String key, final HeuristicCode code, final Confidence confidence, final String anchor) {
        return Map.entry(key, new RuleMapping(code, Severity.WARNING, confidence, SPOTBUGS_DOCS + anchor));
    }

    private static Map.Entry<String, RuleMapping> error(
            final String key, final HeuristicCode code, final String anchor) {
        return Map.entry(key, new RuleMapping(code, Severity.ERROR, Confidence.HIGH, SPOTBUGS_DOCS + anchor));
    }

    private static Map.Entry<String, RuleMapping> info(
            final String key, final HeuristicCode code, final String anchor) {
        return Map.entry(key, new RuleMapping(code, Severity.INFO, Confidence.HIGH, SPOTBUGS_DOCS + anchor));
    }

    private static Map.Entry<String, RuleMapping> info(
            final String key, final HeuristicCode code, final Confidence confidence, final String anchor) {
        return Map.entry(key, new RuleMapping(code, Severity.INFO, confidence, SPOTBUGS_DOCS + anchor));
    }
}
