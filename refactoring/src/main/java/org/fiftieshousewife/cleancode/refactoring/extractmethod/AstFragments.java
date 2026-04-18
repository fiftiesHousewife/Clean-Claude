package org.fiftieshousewife.cleancode.refactoring.extractmethod;

import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;

import java.util.List;
import java.util.Optional;

/**
 * Parses Java source strings into the small AST fragments the extract-method
 * recipe needs to splice into the enclosing compilation unit. Factored out so
 * callers don't repeat the `wrap in a holder class, pick the first method,
 * then the first statement` dance.
 */
final class AstFragments {

    private static final String HOLDER_PREFIX = "class __ExtractMethodRecipe_Holder__ {\n";
    private static final String HOLDER_SUFFIX = "\n}\n";
    private static final Space BETWEEN_METHODS = Space.format("\n\n    ");

    private AstFragments() {}

    static Optional<J.MethodDeclaration> parseMethod(final String methodSource) {
        return firstMethodOf(HOLDER_PREFIX + methodSource + HOLDER_SUFFIX)
                .map(m -> m.withPrefix(BETWEEN_METHODS));
    }

    static Optional<Statement> parseStatement(final String statementSource) {
        final String wrapper = HOLDER_PREFIX + "void __m__() { " + statementSource + " }" + HOLDER_SUFFIX;
        return firstMethodOf(wrapper)
                .filter(m -> m.getBody() != null && !m.getBody().getStatements().isEmpty())
                .map(m -> m.getBody().getStatements().getFirst());
    }

    private static Optional<J.MethodDeclaration> firstMethodOf(final String source) {
        final List<SourceFile> parsed = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build().parse(source).toList();
        if (parsed.isEmpty() || !(parsed.getFirst() instanceof J.CompilationUnit cu)) {
            return Optional.empty();
        }
        if (cu.getClasses().isEmpty()) {
            return Optional.empty();
        }
        final J.ClassDeclaration cls = cu.getClasses().getFirst();
        if (cls.getBody().getStatements().isEmpty()
                || !(cls.getBody().getStatements().getFirst() instanceof J.MethodDeclaration method)) {
            return Optional.empty();
        }
        return Optional.of(method);
    }
}
