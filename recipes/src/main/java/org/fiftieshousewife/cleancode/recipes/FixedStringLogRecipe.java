package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Detects logger calls whose entire payload is a compile-time constant
 * — {@code log.info("starting up")}, {@code log.warn("done")}. Robert
 * Martin classifies these under "clutter" (Clean Code Ch.17 / G12):
 * dev-time noise that survives into production but carries no
 * runtime information for the reader of a log line.
 *
 * <p>Matches when ALL of:
 * <ul>
 *   <li>The receiver is named {@code log}, {@code logger}, {@code LOG},
 *       or {@code LOGGER}, OR has a type whose simple name is
 *       {@code Logger}.</li>
 *   <li>The method name is one of the standard SLF4J levels:
 *       {@code trace}, {@code debug}, {@code info}, {@code warn},
 *       {@code error}.</li>
 *   <li>The call has exactly one argument that is itself a string
 *       literal — possibly built from concatenation of literals only.
 *       Anything that introduces a variable (a non-literal in the
 *       concat, a second {@code MessageFormatter}-style argument)
 *       carries information and is not flagged.</li>
 * </ul>
 */
public class FixedStringLogRecipe extends ScanningRecipe<FixedStringLogRecipe.Accumulator> {

    public record Row(String className, String level, String literal) {}

    public static final class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private static final Set<String> LOG_LEVELS = Set.of(
            "trace", "debug", "info", "warn", "error");
    private static final Set<String> LOGGER_FIELD_NAMES = Set.of(
            "log", "logger", "LOG", "LOGGER");
    private static final String LOGGER_TYPE_SIMPLE_NAME = "Logger";

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Fixed-string log detection (G12)";
    }

    @Override
    public String getDescription() {
        return "Flags logger calls whose entire payload is a compile-time "
                + "constant — `log.info(\"starting up\")` and friends. "
                + "Per Clean Code Ch.17, these are clutter: noise without "
                + "runtime information.";
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        lastAccumulator = new Accumulator();
        return lastAccumulator;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(final J.MethodInvocation method,
                                                            final ExecutionContext ctx) {
                final J.MethodInvocation visited = super.visitMethodInvocation(method, ctx);
                if (!isLogLevelCall(visited) || !isFixedStringPayload(visited)) {
                    return visited;
                }
                final String literal = extractLiteralValue(visited.getArguments().getFirst());
                if (literal == null) {
                    return visited;
                }
                acc.rows.add(new Row(enclosingClassName(),
                        visited.getSimpleName(), literal));
                return visited;
            }

            private String enclosingClassName() {
                final J.ClassDeclaration enclosing = getCursor().firstEnclosing(J.ClassDeclaration.class);
                return enclosing != null ? enclosing.getSimpleName() : "<unknown>";
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<Row> collectedRows() {
        return lastAccumulator != null
                ? Collections.unmodifiableList(lastAccumulator.rows)
                : List.of();
    }

    private static boolean isLogLevelCall(final J.MethodInvocation mi) {
        if (!LOG_LEVELS.contains(mi.getSimpleName())) {
            return false;
        }
        final Expression select = mi.getSelect();
        if (!(select instanceof J.Identifier ident)) {
            return false;
        }
        if (LOGGER_FIELD_NAMES.contains(ident.getSimpleName())) {
            return true;
        }
        // Type-based fallback: when the parser has classpath context the
        // identifier's declared type resolves to the Logger interface and
        // we can flag unconventional field names too. Without classpath
        // (most unit-test environments) getType() returns an Unknown and
        // we fall back to the LOGGER_FIELD_NAMES allowlist above.
        final JavaType type = ident.getType();
        if (type instanceof JavaType.FullyQualified fq) {
            return LOGGER_TYPE_SIMPLE_NAME.equals(fq.getClassName())
                    || fq.getClassName().endsWith("." + LOGGER_TYPE_SIMPLE_NAME);
        }
        return false;
    }

    private static boolean isFixedStringPayload(final J.MethodInvocation mi) {
        if (mi.getArguments().size() != 1) {
            return false;
        }
        return isConstantStringExpression(mi.getArguments().getFirst());
    }

    private static boolean isConstantStringExpression(final Expression expr) {
        if (expr instanceof J.Literal lit) {
            return lit.getType() == JavaType.Primitive.String && lit.getValue() instanceof String;
        }
        if (expr instanceof J.Binary bin && bin.getOperator() == J.Binary.Type.Addition) {
            return isConstantStringExpression(bin.getLeft())
                    && isConstantStringExpression(bin.getRight());
        }
        return false;
    }

    private static String extractLiteralValue(final Expression expr) {
        if (expr instanceof J.Literal lit && lit.getValue() instanceof String s) {
            return s;
        }
        if (expr instanceof J.Binary bin && bin.getOperator() == J.Binary.Type.Addition) {
            final String left = extractLiteralValue(bin.getLeft());
            final String right = extractLiteralValue(bin.getRight());
            if (left != null && right != null) {
                return left + right;
            }
        }
        return null;
    }
}
