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
import java.util.regex.Pattern;

/**
 * Classifies the free variables of an {@link ExtractionTarget} into
 * parameters, output value, and rejection reasons — the same role
 * IntelliJ's {@code InputVariables} + {@code ControlFlowWrapper} play
 * together for the upstream refactor.
 *
 * <p>First-cut constraints are enforced here (no escaping control flow,
 * no reassignment of outer locals, at most one output variable). When
 * any constraint fails, {@link #extractable()} is {@code false} and
 * {@link #rejectionReason()} explains why.
 */
record ExtractionAnalysis(
        List<Parameter> parameters,
        Optional<OutputVariable> outputVariable,
        String returnTypeSource,
        boolean isStatic,
        List<String> throwsList,
        boolean extractable,
        String rejectionReason) {

    record Parameter(String name, String typeSource) {}
    record OutputVariable(String name, String typeSource) {}

    private static final String VOID = "void";
    private static final String VAR = "var";
    private static final Pattern ESCAPE_KEYWORDS = Pattern.compile("\\b(return|break|continue|throw)\\b");

    static ExtractionAnalysis analyse(final ExtractionTarget target, final Cursor cursor) {
        if (target.enclosingMethod().getTypeParameters() != null
                && !target.enclosingMethod().getTypeParameters().isEmpty()) {
            return reject("enclosing method is generic; first-cut port does not propagate type parameters");
        }
        final String rangeText = target.extractedText();
        if (ESCAPE_KEYWORDS.matcher(rangeText).find()) {
            return reject("range contains return/break/continue/throw — control flow would escape");
        }
        final Map<String, String> externalNames = collectExternalNames(target, cursor);
        final List<Parameter> params = new ArrayList<>();
        for (final Map.Entry<String, String> e : externalNames.entrySet()) {
            if (isWritten(rangeText, e.getKey())) {
                return reject("range reassigns outer local `" + e.getKey() + "` (Java has no pass-by-reference)");
            }
            if (isRead(rangeText, e.getKey())) {
                params.add(new Parameter(e.getKey(), e.getValue()));
            }
        }
        final String afterText = target.afterRangeText();
        final List<OutputVariable> outputs = new ArrayList<>();
        collectInternalDecls(target, cursor).forEach(ov -> {
            if (isRead(afterText, ov.name())) {
                outputs.add(ov);
            }
        });
        if (outputs.size() > 1) {
            return reject("more than one local declared in range is used after it — cannot encode in Java return");
        }
        final Optional<OutputVariable> output = outputs.isEmpty()
                ? Optional.empty() : Optional.of(outputs.getFirst());
        if (output.isPresent() && VAR.equals(output.get().typeSource())) {
            return reject("output variable uses `var` — first-cut port requires an explicit declared type");
        }
        return new ExtractionAnalysis(
                params,
                output,
                output.map(OutputVariable::typeSource).orElse(VOID),
                isStatic(target),
                throwsList(target, cursor),
                true,
                "");
    }

    private static ExtractionAnalysis reject(final String reason) {
        return new ExtractionAnalysis(List.of(), Optional.empty(), VOID, false, List.of(), false, reason);
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
                vd.getVariables().forEach(nv -> decls.add(new OutputVariable(nv.getSimpleName(), type)));
            }
        });
        return decls;
    }

    static boolean isRead(final String text, final String name) {
        return Pattern.compile("\\b" + Pattern.quote(name) + "\\b").matcher(text).find();
    }

    static boolean isWritten(final String text, final String name) {
        final String quoted = Pattern.quote(name);
        final String postWrite = "=(?!=)|[+\\-*/%&|^]=|<<=|>>>?=|\\+\\+|--";
        return Pattern.compile("\\b" + quoted + "\\s*(" + postWrite + ")").matcher(text).find()
                || Pattern.compile("(\\+\\+|--)\\s*" + quoted + "\\b").matcher(text).find();
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