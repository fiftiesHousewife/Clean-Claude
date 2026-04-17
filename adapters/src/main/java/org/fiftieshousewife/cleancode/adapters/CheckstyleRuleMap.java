package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.Confidence;
import org.fiftieshousewife.cleancode.core.Severity;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

final class CheckstyleRuleMap {

    record RuleMapping(HeuristicCode code, Severity severity, Confidence confidence, String ruleUrl) {}

    private static final String CHECKSTYLE_DOCS = "https://checkstyle.org/checks/";

    private static final Map<String, RuleMapping> RULES = Map.ofEntries(
            warning("ParameterNumber", HeuristicCode.F1, Confidence.HIGH, "sizes/parameternumber.html"),
            warning("MagicNumber", HeuristicCode.G25, Confidence.HIGH, "coding/magicnumber.html"),
            warning("LocalVariableName", HeuristicCode.N1, Confidence.MEDIUM, "naming/localvariablename.html"),
            warning("MethodLength", HeuristicCode.G30, Confidence.MEDIUM, "sizes/methodlength.html"),
            warning("AnonInnerLength", HeuristicCode.G30, Confidence.MEDIUM, "sizes/anoninnerlength.html"),
            warning("AvoidStarImport", HeuristicCode.J1, Confidence.HIGH, "imports/avoidstarimport.html"),
            warning("IllegalImport", HeuristicCode.G12, Confidence.HIGH, "imports/illegalimport.html"),
            warning("InterfaceIsType", HeuristicCode.J2, Confidence.HIGH, "design/interfaceistype.html"),
            warning("VisibilityModifier", HeuristicCode.G8, Confidence.MEDIUM, "design/visibilitymodifier.html"),
            warning("HideUtilityClassConstructor", HeuristicCode.G18, Confidence.HIGH,
                    "design/hideutilityclassconstructor.html"),
            warning("OneTopLevelClass", HeuristicCode.G12, Confidence.HIGH, "design/onetoplevelclass.html"),
            warning("NeedBraces", HeuristicCode.G24, Confidence.MEDIUM, "blocks/needbraces.html"),
            warning("LeftCurly", HeuristicCode.G24, Confidence.HIGH, "blocks/leftcurly.html"),
            warning("RightCurly", HeuristicCode.G24, Confidence.HIGH, "blocks/rightcurly.html"),
            warning("WhitespaceAround", HeuristicCode.G24, Confidence.HIGH, "whitespace/whitespacearound.html"),
            warning("EmptyLineSeparator", HeuristicCode.G10, Confidence.MEDIUM,
                    "whitespace/emptylineseparator.html"),
            warning("MethodName", HeuristicCode.N1, Confidence.MEDIUM, "naming/methodname.html"),
            warning("TypeName", HeuristicCode.N1, Confidence.MEDIUM, "naming/typename.html"),
            warning("FinalLocalVariable", HeuristicCode.G22, Confidence.HIGH, "coding/finallocalvariable.html"),
            warning("SimplifyBooleanExpression", HeuristicCode.G28, Confidence.HIGH,
                    "coding/simplifybooleanexpression.html"),
            warning("SimplifyBooleanReturn", HeuristicCode.G28, Confidence.HIGH,
                    "coding/simplifybooleanreturn.html"),
            info("RedundantImport", HeuristicCode.G12, "imports/redundantimport.html"),
            info("UnusedImports", HeuristicCode.G12, "imports/unusedimports.html"),
            warning("EmptyBlock", HeuristicCode.G4, Confidence.HIGH, "blocks/emptyblock.html"),
            warning("FileLength", HeuristicCode.Ch10_1, Confidence.MEDIUM, "sizes/filelength.html"),
            warning("LineLength", HeuristicCode.G24, Confidence.HIGH, "sizes/linelength.html")
    );

    private CheckstyleRuleMap() {}

    static Optional<RuleMapping> lookup(final String checkName) {
        return Optional.ofNullable(RULES.get(checkName));
    }

    static Collection<RuleMapping> all() {
        return RULES.values();
    }

    private static Map.Entry<String, RuleMapping> warning(
            final String name, final HeuristicCode code, final Confidence confidence, final String path) {
        return Map.entry(name, new RuleMapping(code, Severity.WARNING, confidence, CHECKSTYLE_DOCS + path));
    }

    private static Map.Entry<String, RuleMapping> info(
            final String name, final HeuristicCode code, final String path) {
        return Map.entry(name, new RuleMapping(code, Severity.INFO, Confidence.HIGH, CHECKSTYLE_DOCS + path));
    }
}
