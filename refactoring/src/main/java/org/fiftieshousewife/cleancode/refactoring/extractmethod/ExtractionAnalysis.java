package org.fiftieshousewife.cleancode.refactoring.extractmethod;

import org.openrewrite.Cursor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeTree;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Classifies the free variables of an {@link ExtractionTarget} into
 * parameters and output — the role IntelliJ's {@code InputVariables}
 * plays — and classifies the range's exit mode — the role
 * {@code ControlFlowWrapper} plays. When any constraint fails,
 * {@link #extractable()} is {@code false} and {@link #rejectionReason()}
 * explains why.
 *
 * <p>Phase A accepts bare {@code return;} inside a void-returning method
 * (see {@link ExitMode#VOID_CONDITIONAL_EXIT}). Phase B treats an outer
 * local that is written inside the range and read after it as an output
 * variable — the extracted method returns the new value and the call
 * site re-assigns.
 */
record ExtractionAnalysis(
        List<Parameter> parameters,
        Optional<OutputVariable> outputVariable,
        String returnTypeSource,
        boolean isStatic,
        List<String> throwsList,
        ExitMode exitMode,
        boolean extractable,
        String rejectionReason) {

    record Parameter(String name, String typeSource) {}
    record OutputVariable(String name, String typeSource, boolean outerWrite) {}

    private static final String VOID = "void";
    private static final String BOOLEAN = "boolean";
    private static final String VAR = "var";

    static ExtractionAnalysis analyse(final ExtractionTarget target, final Cursor cursor) {
        if (target.enclosingMethod().getTypeParameters() != null
                && !target.enclosingMethod().getTypeParameters().isEmpty()) {
            return reject("enclosing method is generic; first-cut port does not propagate type parameters");
        }
        final String rangeText = target.extractedText();
        final String afterText = target.afterRangeText();
        final Optional<ExitMode> maybeMode = ExitMode.classify(rangeText, target.enclosingMethod(), cursor);
        if (maybeMode.isEmpty()) {
            return reject("control-flow escape other than void conditional-exit is not supported yet");
        }
        final ExitMode exitMode = maybeMode.get();
        final Map<String, String> externalNames = collectExternalNames(target, cursor);
        final List<Parameter> params = collectParameters(rangeText, externalNames);
        final List<OutputVariable> outputs = collectOutputs(target, cursor, rangeText, afterText, externalNames);
        if (outputs.size() > 1) {
            return reject("more than one output value is needed — cannot encode in a Java return");
        }
        if (exitMode != ExitMode.NONE && !outputs.isEmpty()) {
            return reject("cannot combine conditional-exit with an output variable in Phase A");
        }
        final Optional<OutputVariable> output = outputs.isEmpty()
                ? Optional.empty() : Optional.of(outputs.getFirst());
        if (output.isPresent() && VAR.equals(output.get().typeSource())) {
            return reject("output variable uses `var` — first-cut port requires an explicit declared type");
        }
        final String returnType = exitMode == ExitMode.VOID_CONDITIONAL_EXIT
                ? BOOLEAN
                : output.map(OutputVariable::typeSource).orElse(VOID);
        return new ExtractionAnalysis(params, output, returnType,
                isStatic(target), throwsList(target, cursor), exitMode, true, "");
    }

    private static List<Parameter> collectParameters(final String rangeText,
                                                     final Map<String, String> externalNames) {
        final List<Parameter> params = new ArrayList<>();
        externalNames.forEach((name, type) -> {
            if (VariableUsagePatterns.isRead(rangeText, name)) {
                params.add(new Parameter(name, type));
            }
        });
        return params;
    }

    private static List<OutputVariable> collectOutputs(final ExtractionTarget target, final Cursor cursor,
                                                       final String rangeText, final String afterText,
                                                       final Map<String, String> externalNames) {
        final List<OutputVariable> outputs = new ArrayList<>();
        collectInternalDecls(target, cursor).forEach(ov -> {
            if (VariableUsagePatterns.isRead(afterText, ov.name())) {
                outputs.add(ov);
            }
        });
        externalNames.forEach((name, type) -> {
            if (VariableUsagePatterns.isWritten(rangeText, name)
                    && VariableUsagePatterns.isRead(afterText, name)) {
                outputs.add(new OutputVariable(name, type, true));
            }
        });
        return outputs;
    }

    private static ExtractionAnalysis reject(final String reason) {
        return new ExtractionAnalysis(List.of(), Optional.empty(), VOID, false, List.of(),
                ExitMode.NONE, false, reason);
    }

    private static Map<String, String> collectExternalNames(final ExtractionTarget target, final Cursor cursor) {
        final Map<String, String> names = new LinkedHashMap<>();
        target.enclosingMethod().getParameters().forEach(p -> recordDecl(p, names, cursor));
        target.statementsBeforeRange().forEach(s -> recordDecl(s, names, cursor));
        return names;
    }

    private static void recordDecl(final Statement s, final Map<String, String> names, final Cursor cursor) {
        if (!(s instanceof J.VariableDeclarations vd) || vd.getTypeExpression() == null) {
            return;
        }
        final String type = vd.getTypeExpression().printTrimmed(cursor);
        vd.getVariables().forEach(nv -> names.put(nv.getSimpleName(), type));
    }

    private static List<OutputVariable> collectInternalDecls(final ExtractionTarget target, final Cursor cursor) {
        final List<OutputVariable> decls = new ArrayList<>();
        target.extractedStatements().forEach(s -> {
            if (s instanceof J.VariableDeclarations vd) {
                final String type = vd.getTypeExpression() == null
                        ? VAR : vd.getTypeExpression().printTrimmed(cursor);
                vd.getVariables().forEach(nv -> decls.add(new OutputVariable(nv.getSimpleName(), type, false)));
            }
        });
        return decls;
    }

    private static boolean isStatic(final ExtractionTarget target) {
        return target.enclosingMethod().getModifiers().stream()
                .anyMatch(m -> m.getType() == J.Modifier.Type.Static);
    }

    private static List<String> throwsList(final ExtractionTarget target, final Cursor cursor) {
        if (target.enclosingMethod().getThrows() == null) {
            return List.of();
        }
        final List<String> out = new ArrayList<>();
        target.enclosingMethod().getThrows().forEach(t ->
                out.add(((TypeTree) t).printTrimmed(cursor)));
        return out;
    }

    List<String> parameterDeclarations() {
        final List<String> out = new ArrayList<>();
        parameters.forEach(p -> out.add(p.typeSource() + " " + p.name()));
        return out;
    }

    List<String> argumentNames() {
        final List<String> out = new ArrayList<>();
        parameters.forEach(p -> out.add(p.name()));
        return out;
    }
}
