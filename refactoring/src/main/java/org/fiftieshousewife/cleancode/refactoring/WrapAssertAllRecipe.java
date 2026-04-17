package org.fiftieshousewife.cleancode.refactoring;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class WrapAssertAllRecipe extends Recipe {

    private static final Set<String> ASSERT_PREFIXES = Set.of(
            "assertThat", "assertEquals", "assertTrue", "assertFalse",
            "assertNull", "assertNotNull", "assertThrows", "assertSame",
            "assertNotEquals", "assertArrayEquals", "assertIterableEquals");

    private final int minConsecutive;

    @JsonCreator
    public WrapAssertAllRecipe(@JsonProperty("minConsecutive") int minConsecutive) {
        this.minConsecutive = minConsecutive;
    }

    @Override
    public String getDisplayName() {
        return "Wrap consecutive assertions in assertAll";
    }

    @Override
    public String getDescription() {
        return "Wraps %d+ consecutive assert statements in assertAll so all failures are reported together. Fixes T1."
                .formatted(minConsecutive);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                final J.Block visitedBlock = super.visitBlock(block, ctx);
                if (!isInsideTestMethod(getCursor())) {
                    return visitedBlock;
                }
                final List<Statement> statements = visitedBlock.getStatements();
                final List<AssertRun> runs = findAssertRuns(statements);
                if (runs.isEmpty()) {
                    return visitedBlock;
                }
                return visitedBlock.withStatements(rewriteStatements(statements, runs));
            }
        };
    }

    private boolean isInsideTestMethod(final org.openrewrite.Cursor cursor) {
        final J.MethodDeclaration enclosingMethod = cursor.firstEnclosing(J.MethodDeclaration.class);
        return enclosingMethod != null && isTestMethod(enclosingMethod);
    }

    private List<Statement> rewriteStatements(final List<Statement> statements, final List<AssertRun> runs) {
        final List<Statement> newStatements = new ArrayList<>();
        int i = 0;
        for (final AssertRun run : runs) {
            while (i < run.startIndex()) {
                newStatements.add(statements.get(i++));
            }
            newStatements.addAll(assertAllOrOriginals(statements, run));
            i = run.endIndex();
        }
        while (i < statements.size()) {
            newStatements.add(statements.get(i++));
        }
        return newStatements;
    }

    private List<Statement> assertAllOrOriginals(final List<Statement> statements, final AssertRun run) {
        final Statement assertAllStmt = buildAssertAllStatement(statements, run);
        if (assertAllStmt != null) {
            return List.of(assertAllStmt.withPrefix(statements.get(run.startIndex()).getPrefix()));
        }
        return statements.subList(run.startIndex(), run.endIndex());
    }

    Statement buildAssertAllStatement(final List<Statement> statements, final AssertRun run) {
        final String lambdas = statements.subList(run.startIndex(), run.endIndex()).stream()
                .map(s -> "() -> " + s.toString().trim())
                .collect(Collectors.joining(",\n                "));
        final String assertAllSource =
                "class _T { void _m() { assertAll(\n                %s\n        ); } }".formatted(lambdas);
        final List<SourceFile> parsed = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build().parse(assertAllSource).toList();
        if (parsed.isEmpty()) {
            return null;
        }
        final J.MethodDeclaration method = (J.MethodDeclaration)
                ((J.CompilationUnit) parsed.getFirst())
                        .getClasses().getFirst().getBody().getStatements().getFirst();
        return method.getBody().getStatements().getFirst();
    }

    private record AssertRun(int startIndex, int endIndex) {}

    List<AssertRun> findAssertRuns(final List<Statement> statements) {
        int runStart = -1;
        int runLength = 0;
        final List<AssertRun> runs = new ArrayList<>();
        for (int i = 0; i < statements.size(); i++) {
            if (isAssertCall(statements.get(i))) {
                if (runStart < 0) {
                    runStart = i;
                }
                runLength++;
            } else {
                if (runLength >= minConsecutive) {
                    runs.add(new AssertRun(runStart, runStart + runLength));
                }
                runStart = -1;
                runLength = 0;
            }
        }
        if (runLength >= minConsecutive) {
            runs.add(new AssertRun(runStart, runStart + runLength));
        }
        return runs;
    }

    private static boolean isAssertCall(final Statement stmt) {
        if (stmt instanceof J.MethodInvocation mi) {
            return ASSERT_PREFIXES.stream().anyMatch(p -> mi.getSimpleName().startsWith(p));
        }
        return false;
    }

    private static boolean isTestMethod(final J.MethodDeclaration method) {
        return method.getLeadingAnnotations().stream()
                .anyMatch(a -> "Test".equals(a.getSimpleName()));
    }
}
