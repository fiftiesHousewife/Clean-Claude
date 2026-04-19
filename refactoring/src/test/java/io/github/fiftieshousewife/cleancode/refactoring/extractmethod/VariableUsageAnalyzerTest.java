package io.github.fiftieshousewife.cleancode.refactoring.extractmethod;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VariableUsageAnalyzerTest {

    @Test
    void readDetectsPlainIdentifier() {
        final List<Statement> stmts = parseMethodBody("int x = count + 1;");
        assertTrue(VariableUsageAnalyzer.isRead(stmts, "count"));
    }

    @Test
    void readDoesNotMatchSubstringOfLongerIdentifier() {
        final List<Statement> decls = parseMethodBody("countryCode = 1;", "int countryCode;");
        assertAll(
                () -> assertFalse(VariableUsageAnalyzer.isRead(decls, "count"),
                        "countryCode is one identifier; no walk should synthesise a sub-read"),
                () -> assertFalse(VariableUsageAnalyzer.isRead(parseMethodBody(
                                "int total = accountId;", "int accountId;"), "count"),
                        "accountId does not contain a read of count"));
    }

    @Test
    void readIgnoresIdentifierInsideCommentOrStringLiteral() {
        final List<Statement> stmts = parseMethodBody(
                "String message = \"count is missing\"; // count also mentioned here");
        assertFalse(VariableUsageAnalyzer.isRead(stmts, "count"),
                "comments and string literals live outside the J.Identifier tree");
    }

    @Test
    void readIgnoresFieldAccessSelector() {
        final List<Statement> stmts = parseMethodBody("int v = obj.count;", "Thing obj = null;");
        assertFalse(VariableUsageAnalyzer.isRead(stmts, "count"),
                "obj.count is a field access, not a read of a local count variable");
    }

    @Test
    void readIgnoresMethodName() {
        final List<Statement> stmts = parseMethodBody("count();", "");
        assertFalse(VariableUsageAnalyzer.isRead(stmts, "count"),
                "count() is a method invocation, not a read of a count variable");
    }

    @Test
    void readIgnoresLhsOfPlainAssignment() {
        final List<Statement> stmts = parseMethodBody("count = 1;", "int count;");
        assertFalse(VariableUsageAnalyzer.isRead(stmts, "count"),
                "`count = 1;` writes to count; the LHS is not a read");
    }

    @Test
    void readAcceptsCompoundAssignmentTargetAsRead() {
        final List<Statement> stmts = parseMethodBody("count += 1;", "int count;");
        assertTrue(VariableUsageAnalyzer.isRead(stmts, "count"),
                "`count += 1` reads count before writing — counts as a read");
    }

    @Test
    void writeDetectsSimpleAssignment() {
        final List<Statement> stmts = parseMethodBody("total = sum;", "int total; int sum;");
        assertTrue(VariableUsageAnalyzer.isWritten(stmts, "total"));
    }

    @Test
    void writeDistinguishesAssignmentFromComparison() {
        assertAll(
                () -> assertFalse(VariableUsageAnalyzer.isWritten(
                        parseMethodBody("if (x == 1) { }", "int x;"), "x")),
                () -> assertFalse(VariableUsageAnalyzer.isWritten(
                        parseMethodBody("boolean b = x != 2;", "int x;"), "x")));
    }

    @Test
    void writeDetectsCompoundAssignments() {
        assertAll(
                () -> assertTrue(VariableUsageAnalyzer.isWritten(
                        parseMethodBody("count += 1;", "int count;"), "count")),
                () -> assertTrue(VariableUsageAnalyzer.isWritten(
                        parseMethodBody("count -= 1;", "int count;"), "count")),
                () -> assertTrue(VariableUsageAnalyzer.isWritten(
                        parseMethodBody("mask |= flag;", "int mask; int flag;"), "mask")),
                () -> assertTrue(VariableUsageAnalyzer.isWritten(
                        parseMethodBody("bits <<= 1;", "int bits;"), "bits")));
    }

    @Test
    void writeDetectsIncrementAndDecrement() {
        assertAll(
                () -> assertTrue(VariableUsageAnalyzer.isWritten(
                        parseMethodBody("count++;", "int count;"), "count")),
                () -> assertTrue(VariableUsageAnalyzer.isWritten(
                        parseMethodBody("--count;", "int count;"), "count")),
                () -> assertTrue(VariableUsageAnalyzer.isWritten(
                        parseMethodBody("++count;", "int count;"), "count")),
                () -> assertTrue(VariableUsageAnalyzer.isWritten(
                        parseMethodBody("count--;", "int count;"), "count")));
    }

    @Test
    void writeIgnoresAssignmentToDifferentName() {
        final List<Statement> stmts = parseMethodBody("other = count;", "int other; int count;");
        assertFalse(VariableUsageAnalyzer.isWritten(stmts, "count"),
                "reading count into another variable is not a write to count");
    }

    @Test
    void writeIgnoresIdentifierInCommentOrStringLiteral() {
        final List<Statement> stmts = parseMethodBody(
                "String s = \"count = 9\"; // count = 10 is hypothetical");
        assertFalse(VariableUsageAnalyzer.isWritten(stmts, "count"),
                "text inside string literals and comments must not register as writes");
    }

    private static List<Statement> parseMethodBody(final String statements) {
        return parseMethodBody(statements, "");
    }

    private static List<Statement> parseMethodBody(final String statements, final String prelude) {
        final String source = """
                package com.example;
                class Fixture {
                    void method() {
                        %s
                        %s
                    }
                }
                """.formatted(prelude, statements);
        final J.CompilationUnit cu = (J.CompilationUnit) JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build()
                .parse(source)
                .findFirst()
                .orElseThrow();
        final J.ClassDeclaration clazz = cu.getClasses().get(0);
        final J.MethodDeclaration method = (J.MethodDeclaration) clazz.getBody().getStatements().get(0);
        return method.getBody().getStatements();
    }
}
