package org.fiftieshousewife.cleancode.refactoring;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EncapsulateBoundaryRecipe extends Recipe {

    private static final Set<String> BOUNDARY_METHODS = Set.of("length", "size");
    private static final String DECL_TEMPLATE = "class _T { void _m() { final int lastIndex = %s; } }";

    @Override
    public String getDisplayName() {
        return "Encapsulate boundary conditions into named variables";
    }

    @Override
    public String getDescription() {
        return "Adds a named variable for boundary arithmetic like array.length - 1. Fixes G33.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                final J.Block visitedBlock = super.visitBlock(block, ctx);
                return rewriteStatements(visitedBlock);
            }
        };
    }

    static J.Block rewriteStatements(final J.Block block) {
        final List<Statement> statements = block.getStatements();
        final List<Statement> newStatements = new ArrayList<>();
        boolean changed = false;

        for (final Statement stmt : statements) {
            final Statement lastIndexDecl = buildLastIndexDeclaration(stmt);
            if (lastIndexDecl == null) {
                newStatements.add(stmt);
                continue;
            }
            newStatements.add(lastIndexDecl);
            newStatements.add(stmt);
            changed = true;
        }

        return changed ? block.withStatements(newStatements) : block;
    }

    static Statement buildLastIndexDeclaration(final Statement stmt) {
        final String boundaryExpr = extractBoundaryText(stmt);
        if (boundaryExpr == null) {
            return null;
        }
        final Statement parsedDecl = parseLastIndexDecl(boundaryExpr);
        if (parsedDecl == null) {
            return null;
        }
        return parsedDecl.withPrefix(stmt.getPrefix());
    }

    static Statement parseLastIndexDecl(final String boundaryExpr) {
        final String declSource = DECL_TEMPLATE.formatted(boundaryExpr);
        final List<SourceFile> parsed = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build().parse(declSource).toList();
        if (parsed.isEmpty()) {
            return null;
        }
        final J.CompilationUnit compilationUnit = (J.CompilationUnit) parsed.getFirst();
        final J.MethodDeclaration method = (J.MethodDeclaration)
                compilationUnit.getClasses().getFirst().getBody().getStatements().getFirst();
        return method.getBody().getStatements().getFirst();
    }

    static String extractBoundaryText(final Statement stmt) {
        final BoundaryCollector collector = new BoundaryCollector();
        collector.visit(stmt, 0);
        if (collector.matches.isEmpty()) {
            return null;
        }
        final J.Binary match = collector.matches.getFirst();
        final String left = match.getLeft().toString().trim();
        return left + " - 1";
    }

    private static final class BoundaryCollector extends JavaIsoVisitor<Integer> {
        private final List<J.Binary> matches = new ArrayList<>();

        @Override
        public J.Binary visitBinary(final J.Binary binary, final Integer unused) {
            if (isBoundaryMinusOne(binary)) {
                matches.add(binary);
            }
            return super.visitBinary(binary, unused);
        }
    }

    private static boolean isBoundaryMinusOne(final J.Binary binary) {
        return binary.getOperator() == J.Binary.Type.Subtraction
                && isLiteralOne(binary.getRight())
                && isBoundaryAccess(binary.getLeft());
    }

    private static boolean isLiteralOne(final Expression expr) {
        return expr instanceof J.Literal lit
                && lit.getValue() instanceof Integer value
                && value == 1;
    }

    private static boolean isBoundaryAccess(final Expression expr) {
        return switch (expr) {
            case J.FieldAccess fa -> BOUNDARY_METHODS.contains(fa.getSimpleName());
            case J.MethodInvocation mi -> BOUNDARY_METHODS.contains(mi.getSimpleName());
            default -> false;
        };
    }
}
