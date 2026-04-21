package io.github.fiftieshousewife.cleancode.refactoring;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.List;

/**
 * Rewrites the specific, common idiom
 *
 * <pre>
 *   StringBuilder sb = new StringBuilder();
 *   sb.append(...);
 *   sb.append(...).append(...);
 *   return sb.toString();
 * </pre>
 *
 * into a single {@code return} of a text block, optionally with
 * {@code .formatted(...)} for interpolated pieces. Targets the pattern
 * the skill {@code clean-code-java-idioms} documents as the top reason
 * {@code StringBuilder} is misapplied: building a mostly-literal, mostly
 * multi-line fragment by threading appends that would read as the output
 * if written as a text block.
 *
 * <p>The recipe is deliberately conservative. It only fires when:
 * <ul>
 *   <li>The enclosing block has exactly the three-part shape:
 *       [{@code StringBuilder name = new StringBuilder();}],
 *       [one or more {@code name.append(...)} expression statements,
 *       possibly chained], [{@code return name.toString();}].</li>
 *   <li>The {@code new StringBuilder()} constructor takes no arguments
 *       (a capacity hint or a seed string signals intent that this
 *       recipe should not override).</li>
 *   <li>Each {@code .append(...)} call has exactly one argument.</li>
 *   <li>The assembled literal content contains at least one newline and
 *       ends with a newline — the text block form is only a readability
 *       win when the output is multi-line.</li>
 *   <li>No literal argument contains a tab character or a {@code """}
 *       run (escaping those correctly inside a text block is fiddly and
 *       outside the conservative target).</li>
 * </ul>
 *
 * <p>If any precondition fails, the block is left alone — the skill
 * catches the broader patterns.
 */
public final class ReplaceStringBuilderWithTextBlockRecipe extends Recipe {

    private static final String STRING_BUILDER = "java.lang.StringBuilder";
    private static final String TEXT_BLOCK_INDENT = "        ";

    @Override
    public String getDisplayName() {
        return "Replace StringBuilder-then-toString with a text-block return";
    }

    @Override
    public String getDescription() {
        return "Rewrites `StringBuilder sb = new StringBuilder(); sb.append(...)...; return sb.toString();` "
                + "as a `return \"\"\"text block\"\"\".formatted(...)` when the assembled content is "
                + "multi-line. Intentionally conservative: only the exact three-statement shape fires.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.Block visitBlock(final J.Block block, final ExecutionContext ctx) {
                final J.Block visited = super.visitBlock(block, ctx);
                final Candidate candidate = classify(visited);
                if (candidate == null) {
                    return visited;
                }
                final Statement originalReturn = visited.getStatements().get(visited.getStatements().size() - 1);
                final String returnSource = "return " + buildReturnExpression(candidate) + ";";
                final J.Block rewritten = JavaTemplate.builder(returnSource)
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), originalReturn.getCoordinates().replace(),
                                candidate.interpolations.toArray());
                final List<Statement> rewrittenStmts = rewritten.getStatements();
                final Statement newReturn = rewrittenStmts.get(rewrittenStmts.size() - 1);
                return rewritten.withStatements(List.of(newReturn));
            }
        };
    }

    private static Candidate classify(final J.Block block) {
        final List<Statement> stmts = block.getStatements();
        if (stmts.size() < 3) {
            return null;
        }
        final String builderName = builderDeclarationName(stmts.get(0));
        if (builderName == null) {
            return null;
        }
        if (!isBuilderToStringReturn(stmts.get(stmts.size() - 1), builderName)) {
            return null;
        }
        final List<Expression> appendArgs = new ArrayList<>();
        for (int i = 1; i < stmts.size() - 1; i++) {
            if (!(stmts.get(i) instanceof J.MethodInvocation mi)) {
                return null;
            }
            if (!collectAppendChain(mi, builderName, appendArgs)) {
                return null;
            }
        }
        if (appendArgs.isEmpty()) {
            return null;
        }
        return buildCandidate(appendArgs);
    }

    private static String builderDeclarationName(final Statement stmt) {
        if (!(stmt instanceof J.VariableDeclarations vd) || !isStringBuilderType(vd.getType())) {
            return null;
        }
        if (vd.getVariables().size() != 1) {
            return null;
        }
        final J.VariableDeclarations.NamedVariable var = vd.getVariables().getFirst();
        if (!(var.getInitializer() instanceof J.NewClass nc)) {
            return null;
        }
        if (!isStringBuilderType(nc.getType())) {
            return null;
        }
        if (!isNoArgConstructor(nc)) {
            return null;
        }
        return var.getSimpleName();
    }

    private static boolean isNoArgConstructor(final J.NewClass nc) {
        final List<Expression> args = nc.getArguments();
        if (args == null || args.isEmpty()) {
            return true;
        }
        return args.size() == 1 && args.getFirst() instanceof J.Empty;
    }

    private static boolean isBuilderToStringReturn(final Statement stmt, final String builderName) {
        if (!(stmt instanceof J.Return ret)) {
            return false;
        }
        if (!(ret.getExpression() instanceof J.MethodInvocation toString)) {
            return false;
        }
        if (!"toString".equals(toString.getSimpleName())) {
            return false;
        }
        final List<Expression> args = toString.getArguments();
        if (args != null && !args.isEmpty() && !(args.size() == 1 && args.getFirst() instanceof J.Empty)) {
            return false;
        }
        return toString.getSelect() instanceof J.Identifier id && builderName.equals(id.getSimpleName());
    }

    private static boolean collectAppendChain(final J.MethodInvocation topCall, final String builderName,
                                              final List<Expression> collected) {
        final List<J.MethodInvocation> chain = new ArrayList<>();
        J.MethodInvocation current = topCall;
        while (true) {
            if (!"append".equals(current.getSimpleName())) {
                return false;
            }
            final List<Expression> args = current.getArguments();
            if (args == null || args.size() != 1 || args.getFirst() instanceof J.Empty) {
                return false;
            }
            chain.add(0, current);
            final Expression select = current.getSelect();
            if (select instanceof J.Identifier id) {
                if (!builderName.equals(id.getSimpleName())) {
                    return false;
                }
                break;
            }
            if (select instanceof J.MethodInvocation inner) {
                current = inner;
                continue;
            }
            return false;
        }
        for (final J.MethodInvocation call : chain) {
            collected.add(call.getArguments().getFirst());
        }
        return true;
    }

    private static Candidate buildCandidate(final List<Expression> appendArgs) {
        final List<String> literalPieces = new ArrayList<>();
        final List<Expression> interpolations = new ArrayList<>();
        final StringBuilder current = new StringBuilder();
        for (final Expression arg : appendArgs) {
            if (arg instanceof J.Literal lit && lit.getValue() instanceof String s) {
                if (s.contains("\t") || s.contains("\"\"\"")) {
                    return null;
                }
                current.append(s);
            } else {
                literalPieces.add(current.toString());
                current.setLength(0);
                interpolations.add(arg);
            }
        }
        literalPieces.add(current.toString());
        final String content = assembleContent(literalPieces, interpolations.size());
        if (!content.endsWith("\n") || content.indexOf('\n') == content.length() - 1) {
            return null;
        }
        return new Candidate(content, interpolations);
    }

    private static String assembleContent(final List<String> literalPieces, final int interpolationCount) {
        final boolean escapePercent = interpolationCount > 0;
        final StringBuilder content = new StringBuilder();
        for (int i = 0; i < literalPieces.size(); i++) {
            final String piece = literalPieces.get(i);
            content.append(escapePercent ? piece.replace("%", "%%") : piece);
            if (i < interpolationCount) {
                content.append("%s");
            }
        }
        return content.toString();
    }

    private static String buildReturnExpression(final Candidate candidate) {
        final String contentMinusTrailingNewline =
                candidate.content.substring(0, candidate.content.length() - 1);
        final String[] lines = contentMinusTrailingNewline.split("\n", -1);
        final StringBuilder out = new StringBuilder();
        out.append("\"\"\"\n");
        for (final String line : lines) {
            out.append(TEXT_BLOCK_INDENT).append(escapeLineForTextBlock(line)).append('\n');
        }
        out.append(TEXT_BLOCK_INDENT).append("\"\"\"");
        if (!candidate.interpolations.isEmpty()) {
            out.append(".formatted(");
            for (int i = 0; i < candidate.interpolations.size(); i++) {
                if (i > 0) {
                    out.append(", ");
                }
                out.append("#{any()}");
            }
            out.append(")");
        }
        return out.toString();
    }

    private static String escapeLineForTextBlock(final String line) {
        final StringBuilder out = new StringBuilder(line.length());
        for (int i = 0; i < line.length(); i++) {
            final char c = line.charAt(i);
            if (c == '\\') {
                out.append("\\\\");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static boolean isStringBuilderType(final JavaType type) {
        return type instanceof JavaType.FullyQualified fq
                && STRING_BUILDER.equals(fq.getFullyQualifiedName());
    }

    private record Candidate(String content, List<Expression> interpolations) {}
}
