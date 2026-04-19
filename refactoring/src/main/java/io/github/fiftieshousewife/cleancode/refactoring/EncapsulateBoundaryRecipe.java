package io.github.fiftieshousewife.cleancode.refactoring;

import io.github.fiftieshousewife.cleancode.refactoring.support.AstFragments;
import io.github.fiftieshousewife.cleancode.refactoring.support.Statements;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class EncapsulateBoundaryRecipe extends Recipe {

    private static final Set<String> BOUNDARY_METHODS = Set.of("length", "size");

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
                final J.Block b = super.visitBlock(block, ctx);
                return Statements.rebuild(b, stmt -> {
                    final String boundaryExpr = extractBoundaryText(stmt);
                    if (boundaryExpr == null) {
                        return List.of(stmt);
                    }
                    final Optional<Statement> decl = AstFragments.parseStatement(
                            "final int lastIndex = %s;".formatted(boundaryExpr));
                    if (decl.isEmpty()) {
                        return List.of(stmt);
                    }
                    return List.of(decl.get().withPrefix(stmt.getPrefix()), stmt);
                });
            }
        };
    }

    static String extractBoundaryText(Statement stmt) {
        final List<J.Binary> matches = new ArrayList<>();
        new JavaIsoVisitor<List<J.Binary>>() {
            @Override
            public J.Binary visitBinary(J.Binary binary, List<J.Binary> out) {
                if (isBoundaryMinusOne(binary)) {
                    out.add(binary);
                }
                return super.visitBinary(binary, out);
            }
        }.visit(stmt, matches);
        if (matches.isEmpty()) {
            return null;
        }
        final J.Binary match = matches.getFirst();
        final String left = match.getLeft().toString().trim();
        return left + " - 1";
    }

    private static boolean isBoundaryMinusOne(J.Binary binary) {
        if (binary.getOperator() != J.Binary.Type.Subtraction) {
            return false;
        }
        if (!(binary.getRight() instanceof J.Literal lit) || !(lit.getValue() instanceof Integer i) || i != 1) {
            return false;
        }
        return isBoundaryAccess(binary.getLeft());
    }

    private static boolean isBoundaryAccess(Expression expr) {
        if (expr instanceof J.FieldAccess fa) {
            return BOUNDARY_METHODS.contains(fa.getSimpleName());
        }
        if (expr instanceof J.MethodInvocation mi) {
            return BOUNDARY_METHODS.contains(mi.getSimpleName());
        }
        return false;
    }
}
