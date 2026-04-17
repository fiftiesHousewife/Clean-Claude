package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FullyQualifiedReferenceRecipe extends ScanningRecipe<FullyQualifiedReferenceRecipe.Accumulator> {

    public record Row(String sourceFile, int count, String samplePreview) {}

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
    public Accumulator getInitialValue(ExecutionContext ctx) {
        lastAccumulator = new Accumulator();
        return lastAccumulator;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(final Accumulator acc) {
        return new JavaIsoVisitor<>() {
            private final Map<String, List<String>> perFile = new HashMap<>();
            private String currentSource;

            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                currentSource = cu.getSourcePath().toString();
                perFile.putIfAbsent(currentSource, new ArrayList<>());
                final J.CompilationUnit result = super.visitCompilationUnit(cu, ctx);
                final List<String> samples = perFile.getOrDefault(currentSource, List.of());
                if (!samples.isEmpty()) {
                    acc.rows.add(new Row(currentSource, samples.size(), samples.getFirst()));
                    perFile.remove(currentSource);
                }
                return result;
            }

            @Override
            public J.Import visitImport(J.Import anImport, ExecutionContext ctx) {
                return anImport;
            }

            @Override
            public J.Package visitPackage(J.Package pkg, ExecutionContext ctx) {
                return pkg;
            }

            @Override
            public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                final J.FieldAccess fa = super.visitFieldAccess(fieldAccess, ctx);
                if (isOutermost() && isFullyQualifiedTypeReference(fa)) {
                    final List<String> samples = perFile.get(currentSource);
                    if (samples != null) {
                        samples.add(fa.printTrimmed(getCursor()));
                    }
                }
                return fa;
            }

            private boolean isOutermost() {
                return !(getCursor().getParentOrThrow().getValue() instanceof J.FieldAccess);
            }

            private boolean isFullyQualifiedTypeReference(J.FieldAccess fa) {
                if (!(fa.getType() instanceof JavaType.FullyQualified fq)) {
                    return false;
                }
                return fq.getFullyQualifiedName().contains(".");
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return TreeVisitor.noop();
    }

    public List<Row> collectedRows() {
        return lastAccumulator != null ? Collections.unmodifiableList(lastAccumulator.rows) : List.of();
    }
}
