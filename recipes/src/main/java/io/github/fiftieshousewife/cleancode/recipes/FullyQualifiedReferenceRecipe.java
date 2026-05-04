package io.github.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FullyQualifiedReferenceRecipe extends ScanningRecipe<FullyQualifiedReferenceRecipe.Accumulator> {

    public record Row(String sourceFile, String fqText) {}

    public static class Accumulator {
        final List<Row> rows = new ArrayList<>();
    }

    private Accumulator lastAccumulator;

    @Override
    public String getDisplayName() {
        return "Fully-qualified type reference detection (G12)";
    }

    @Override
    public String getDescription() {
        return "Detects inline `java.util.List` style type references that could be replaced "
                + "with an import + short name. Fixed by ShortenFullyQualifiedReferencesRecipe.";
    }

    @Override
    public Accumulator getInitialValue(final ExecutionContext ctx) {
        lastAccumulator = new Accumulator();
        return lastAccumulator;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(final Accumulator acc) {
        return new JavaIsoVisitor<>() {
            private String currentSource;

            @Override
            public J.CompilationUnit visitCompilationUnit(final J.CompilationUnit cu, final ExecutionContext ctx) {
                currentSource = cu.getSourcePath().toString();
                return super.visitCompilationUnit(cu, ctx);
            }

            @Override
            public J.Import visitImport(final J.Import anImport, final ExecutionContext ctx) {
                return anImport;
            }

            @Override
            public J.Package visitPackage(final J.Package pkg, final ExecutionContext ctx) {
                return pkg;
            }

            @Override
            public J.FieldAccess visitFieldAccess(final J.FieldAccess fieldAccess, final ExecutionContext ctx) {
                final J.FieldAccess fa = super.visitFieldAccess(fieldAccess, ctx);
                if (isOutermost() && isFullyQualifiedTypeReference(fa)) {
                    acc.rows.add(new Row(currentSource, fa.printTrimmed(getCursor())));
                }
                return fa;
            }

            private boolean isOutermost() {
                return !(getCursor().getParentOrThrow().getValue() instanceof J.FieldAccess);
            }

            private boolean isFullyQualifiedTypeReference(final J.FieldAccess fa) {
                if (!(fa.getType() instanceof JavaType.FullyQualified fq)) {
                    return false;
                }
                if (fq.getOwningClass() != null) {
                    return false;
                }
                if ("class".equals(fa.getName().getSimpleName())) {
                    return false;
                }
                if (!fq.getFullyQualifiedName().contains(".")) {
                    return false;
                }
                // The printed source must literally equal the type's
                // fully-qualified name. That's the signature of a real
                // FQ type reference like `java.util.List` — the access
                // expression IS the FQN. For any field / method access
                // (this.x, CLAUDE.x, foo.bar) where x's TYPE is
                // FullyQualified the expression text and the type's
                // FQN diverge, so we don't fire.
                final String printedRoot = stripGenericsAndArrays(fa.printTrimmed(getCursor()));
                return printedRoot.equals(fq.getFullyQualifiedName());
            }

            private static String stripGenericsAndArrays(final String text) {
                int end = text.length();
                final int gen = text.indexOf('<');
                if (gen >= 0) {
                    end = Math.min(end, gen);
                }
                final int arr = text.indexOf('[');
                if (arr >= 0) {
                    end = Math.min(end, arr);
                }
                return text.substring(0, end).trim();
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(final Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<Row> collectedRows() {
        return lastAccumulator != null ? Collections.unmodifiableList(lastAccumulator.rows) : List.of();
    }
}
