package io.github.fiftieshousewife.cleancode.refactoring;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.fiftieshousewife.cleancode.refactoring.support.AstFragments;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
                final J.Block b = super.visitBlock(block, ctx);

                final J.MethodDeclaration enclosingMethod = getCursor().firstEnclosing(J.MethodDeclaration.class);
                if (enclosingMethod == null || !isTestMethod(enclosingMethod)) {
                    return b;
                }

                final List<Statement> statements = b.getStatements();
                final List<AssertRun> runs = findAssertRuns(statements);

                if (runs.isEmpty()) {
                    return b;
                }

                final List<Statement> newStatements = new ArrayList<>();
                int i = 0;
                for (final AssertRun run : runs) {
                    while (i < run.startIndex()) {
                        newStatements.add(statements.get(i++));
                    }

                    final String lambdas = statements.subList(run.startIndex(), run.endIndex()).stream()
                            .map(s -> "() -> " + s.toString().trim())
                            .collect(Collectors.joining(",\n                "));

                    final Optional<Statement> assertAllStmt = AstFragments.parseStatement(
                            "assertAll(\n                %s\n        );".formatted(lambdas));

                    if (assertAllStmt.isPresent()) {
                        newStatements.add(assertAllStmt.get()
                                .withPrefix(statements.get(run.startIndex()).getPrefix()));
                    } else {
                        for (int j = run.startIndex(); j < run.endIndex(); j++) {
                            newStatements.add(statements.get(j));
                        }
                    }

                    i = run.endIndex();
                }

                while (i < statements.size()) {
                    newStatements.add(statements.get(i++));
                }

                return b.withStatements(newStatements);
            }
        };
    }

    private record AssertRun(int startIndex, int endIndex) {}

    private List<AssertRun> findAssertRuns(List<Statement> statements) {
        final List<AssertRun> runs = new ArrayList<>();
        int runStart = -1;
        int runLength = 0;

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

    private static boolean isAssertCall(Statement stmt) {
        if (stmt instanceof J.MethodInvocation mi) {
            return ASSERT_PREFIXES.stream().anyMatch(p -> mi.getSimpleName().startsWith(p));
        }
        return false;
    }

    private static boolean isTestMethod(J.MethodDeclaration method) {
        return method.getLeadingAnnotations().stream()
                .anyMatch(a -> "Test".equals(a.getSimpleName()));
    }
}
