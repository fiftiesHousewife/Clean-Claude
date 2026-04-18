package org.fiftieshousewife.cleancode.refactoring.extractmethod;

import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.List;
import java.util.Optional;

/**
 * The piece of an AST that the extract-method recipe will operate on:
 * the enclosing class, the enclosing method, its body, the inclusive
 * index range of top-level statements to extract, and the original
 * source-text slices for the range and the region after it.
 *
 * <p>IntelliJ's processor represents this via {@code myElements} plus
 * the enclosing {@code PsiMethod} and source offsets. The record
 * carries the same roles. Source slices come from the compilation
 * unit's print, so they retain the original semicolons and indentation
 * — {@code stmt.print(cursor)} drops the semicolon (it is held by the
 * enclosing block's right-padding), which is no good for regenerating
 * a body or searching for reads/writes.
 */
record ExtractionTarget(
        J.ClassDeclaration enclosingClass,
        J.MethodDeclaration enclosingMethod,
        J.Block body,
        int firstStatementIndex,
        int lastStatementIndexInclusive,
        String extractedText,
        String afterRangeText,
        int extractedStartOffset,
        int extractedEndOffset,
        int bodyCloseBraceOffset) {

    List<Statement> extractedStatements() {
        return body.getStatements().subList(firstStatementIndex, lastStatementIndexInclusive + 1);
    }

    List<Statement> statementsBeforeRange() {
        return body.getStatements().subList(0, firstStatementIndex);
    }

    List<Statement> statementsAfterRange() {
        return body.getStatements().subList(lastStatementIndexInclusive + 1, body.getStatements().size());
    }

    static Optional<ExtractionTarget> findIn(final J.CompilationUnit cu, final String cuText,
                                              final LineIndex lines,
                                              final int startLine, final int endLine) {
        return ExtractionTargetFinder.find(cu, cuText, lines, startLine, endLine);
    }
}
