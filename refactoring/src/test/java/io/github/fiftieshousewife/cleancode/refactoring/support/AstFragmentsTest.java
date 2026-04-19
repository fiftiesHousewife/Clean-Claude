package io.github.fiftieshousewife.cleancode.refactoring.support;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AstFragmentsTest {

    @Test
    void parsesSimpleMethodSource() {
        final Optional<J.MethodDeclaration> parsed = AstFragments.parseMethod(
                "private int doubleIt(int n) { return n * 2; }");
        assertTrue(parsed.isPresent(), "well-formed method source should parse");
        assertAll(
                () -> assertEquals("doubleIt", parsed.get().getSimpleName()),
                () -> assertEquals(1, parsed.get().getParameters().size(),
                        "parameter list preserved through the holder-class round trip"));
    }

    @Test
    void parsesVoidMethodWithMultipleStatements() {
        final Optional<J.MethodDeclaration> parsed = AstFragments.parseMethod(
                "private void greet(String name) { int len = name.length(); System.out.println(len); }");
        assertTrue(parsed.isPresent());
        assertEquals(2, parsed.get().getBody().getStatements().size());
    }

    @Test
    void unparseableMethodSourceYieldsEmpty() {
        final Optional<J.MethodDeclaration> parsed = AstFragments.parseMethod(
                "this is not a java method");
        assertTrue(parsed.isEmpty(),
                "garbage input returns empty so callers can fall back rather than throwing");
    }

    @Test
    void parsesStatementWithoutOutputVariable() {
        final Optional<Statement> parsed = AstFragments.parseStatement("doSomething(a, b);");
        assertTrue(parsed.isPresent());
        assertTrue(parsed.get() instanceof J.MethodInvocation
                        || parsed.get().toString().contains("doSomething"),
                "should round-trip to a method-invocation statement");
    }

    @Test
    void parsesVariableDeclarationStatement() {
        final Optional<Statement> parsed = AstFragments.parseStatement("int total = compute(x);");
        assertTrue(parsed.isPresent());
        assertTrue(parsed.get() instanceof J.VariableDeclarations,
                "assignment-from-call should round-trip as a variable declaration statement");
    }

    @Test
    void parsesFieldDeclarationFromHolderClass() {
        final Optional<Statement> parsed = AstFragments.parseField(
                "class _Tmp { private static final String GREETING = \"hi\"; }");
        assertTrue(parsed.isPresent());
        assertTrue(parsed.get() instanceof J.VariableDeclarations,
                "field declarations round-trip as variable-declaration statements");
    }

    @Test
    void parseFieldReturnsEmptyWhenHolderIsMalformed() {
        final Optional<Statement> parsed = AstFragments.parseField("not a class");
        assertTrue(parsed.isEmpty(),
                "malformed input returns empty so callers can fall back");
    }
}
