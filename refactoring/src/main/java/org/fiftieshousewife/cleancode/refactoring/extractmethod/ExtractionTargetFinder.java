package org.fiftieshousewife.cleancode.refactoring.extractmethod;

import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.List;
import java.util.Optional;

/**
 * Walks a compilation unit looking for the single method whose body
 * contains a contiguous run of top-level statements that line up
 * exactly with the requested {@code [startLine, endLine]} range. When
 * found, captures the original source-text slice for the range (from
 * {@code cu.printAll()}) so downstream analysis doesn't need to
 * re-print individual statements (which would drop the trailing
 * semicolons carried on the enclosing block's right-padding).
 */
final class ExtractionTargetFinder {

    private ExtractionTargetFinder() {}

    static Optional<ExtractionTarget> find(final J.CompilationUnit cu, final String cuText,
                                           final LineIndex lines,
                                           final int startLine, final int endLine) {
        final Visitor visitor = new Visitor(cuText, lines, startLine, endLine);
        visitor.visit(cu, null);
        return Optional.ofNullable(visitor.found);
    }

    private static final class Visitor extends JavaIsoVisitor<Object> {
        private final String cuText;
        private final LineIndex lines;
        private final int startLine;
        private final int endLine;
        private ExtractionTarget found;

        Visitor(final String cuText, final LineIndex lines, final int startLine, final int endLine) {
            this.cuText = cuText;
            this.lines = lines;
            this.startLine = startLine;
            this.endLine = endLine;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(final J.MethodDeclaration method,
                                                          final Object ignored) {
            if (found != null || method.getBody() == null) {
                return method;
            }
            final J.ClassDeclaration enclosingClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
            if (enclosingClass == null) {
                return method;
            }
            tryMatchWithin(enclosingClass, method);
            return method;
        }

        private void tryMatchWithin(final J.ClassDeclaration enclosingClass,
                                    final J.MethodDeclaration method) {
            final String methodText = method.print(getCursor());
            final int methodOffset = cuText.indexOf(methodText);
            if (methodOffset < 0) {
                return;
            }
            final int bodyOpenBrace = methodOffset + methodText.indexOf('{');
            final int bodyCloseBrace = methodOffset + methodText.lastIndexOf('}');
            if (bodyOpenBrace <= methodOffset || bodyCloseBrace <= bodyOpenBrace) {
                return;
            }
            final List<Statement> statements = method.getBody().getStatements();
            final int n = statements.size();
            final int[] starts = new int[n];
            final int[] ends = new int[n];
            if (!computeStatementOffsets(statements, bodyOpenBrace + 1, starts, ends)) {
                return;
            }
            final Range range = matchRange(n, starts, ends);
            if (range == null) {
                return;
            }
            recordTarget(enclosingClass, method, bodyCloseBrace, starts, range);
        }

        private void recordTarget(final J.ClassDeclaration enclosingClass,
                                  final J.MethodDeclaration method,
                                  final int bodyCloseBrace,
                                  final int[] starts, final Range range) {
            final int extractedStart = starts[range.first];
            final int extractedEnd = (range.last + 1 < starts.length)
                    ? starts[range.last + 1]
                    : bodyCloseBrace;
            final String extractedText = cuText.substring(extractedStart, extractedEnd);
            final String afterRangeText = cuText.substring(extractedEnd, bodyCloseBrace);
            found = new ExtractionTarget(enclosingClass, method, method.getBody(),
                    range.first, range.last, extractedText, afterRangeText);
        }

        private boolean computeStatementOffsets(final List<Statement> statements, final int searchFromOffset,
                                                final int[] contentStarts, final int[] contentEnds) {
            int searchFrom = searchFromOffset;
            for (int i = 0; i < statements.size(); i++) {
                final String stmtText = statements.get(i).print(getCursor());
                final int offset = cuText.indexOf(stmtText, searchFrom);
                if (offset < 0) {
                    return false;
                }
                final int leadingWs = leadingWhitespaceLength(stmtText);
                contentStarts[i] = offset + leadingWs;
                contentEnds[i] = offset + stmtText.length();
                searchFrom = contentEnds[i];
            }
            return true;
        }

        private Range matchRange(final int n, final int[] starts, final int[] ends) {
            int firstIdx = -1;
            for (int i = 0; i < n; i++) {
                if (lines.lineOf(starts[i]) == startLine) {
                    firstIdx = i;
                    break;
                }
            }
            if (firstIdx < 0) {
                return null;
            }
            for (int i = firstIdx; i < n; i++) {
                final int endLineOfStmt = lines.lineOf(Math.max(starts[i], ends[i] - 1));
                if (endLineOfStmt == endLine) {
                    return new Range(firstIdx, i);
                }
                if (endLineOfStmt > endLine) {
                    return null;
                }
            }
            return null;
        }

        private static int leadingWhitespaceLength(final String text) {
            int i = 0;
            while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
                i++;
            }
            return i;
        }

        private record Range(int first, int last) {}
    }
}
