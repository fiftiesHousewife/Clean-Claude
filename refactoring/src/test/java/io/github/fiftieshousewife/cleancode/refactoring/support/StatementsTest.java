package io.github.fiftieshousewife.cleancode.refactoring.support;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class StatementsTest {

    @Test
    void returnsOriginalBlockWhenExpanderKeepsEveryStatement() {
        final J.Block block = parseMethodBody("int a = 1; int b = 2;");
        final J.Block result = Statements.rebuild(block, List::of);
        assertSame(block, result, "no change means caller gets the same block back");
    }

    @Test
    void expandsOneStatementIntoTwo() {
        final J.Block block = parseMethodBody("int a = 1; int b = 2;");
        final Statement extra = AstFragments.parseStatement("int c = 3;").orElseThrow();
        final J.Block result = Statements.rebuild(block, stmt -> {
            if (stmt.toString().trim().startsWith("int a")) {
                return List.of(extra, stmt);
            }
            return List.of(stmt);
        });
        assertAll(
                () -> assertEquals(3, result.getStatements().size()),
                () -> assertEquals("int c = 3", result.getStatements().get(0).toString().trim()),
                () -> assertEquals("int a = 1", result.getStatements().get(1).toString().trim()));
    }

    @Test
    void dropsStatementsWhenExpanderReturnsEmpty() {
        final J.Block block = parseMethodBody("int keep = 1; int drop = 2; int alsoKeep = 3;");
        final J.Block result = Statements.rebuild(block, stmt ->
                stmt.toString().contains("drop") ? List.of() : List.of(stmt));
        assertEquals(2, result.getStatements().size());
    }

    private static J.Block parseMethodBody(final String statements) {
        final String source = """
                class Fixture {
                    void method() {
                        %s
                    }
                }
                """.formatted(statements);
        final J.CompilationUnit cu = (J.CompilationUnit) JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build()
                .parse(source)
                .findFirst()
                .orElseThrow();
        final J.MethodDeclaration method = (J.MethodDeclaration)
                cu.getClasses().getFirst().getBody().getStatements().getFirst();
        return method.getBody();
    }
}
