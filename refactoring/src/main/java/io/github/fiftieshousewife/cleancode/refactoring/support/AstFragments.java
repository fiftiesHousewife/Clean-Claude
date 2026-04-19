package io.github.fiftieshousewife.cleancode.refactoring.support;

import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;

import java.util.List;
import java.util.Optional;

/**
 * Parses Java source strings into the small AST fragments that
 * refactoring recipes need to splice into an existing compilation unit.
 * Callers avoid repeating the {@code "wrap in holder class, find first
 * method / statement / field"} dance.
 *
 * <p>Hoisted from the extract-method package during D14-2. Every
 * refactoring recipe that parses an isolated fragment should come
 * through here.
 */
public final class AstFragments {

    private static final String HOLDER_PREFIX = "class __RecipeFragmentHolder__ {\n";
    private static final String HOLDER_SUFFIX = "\n}\n";
    private static final Space BETWEEN_METHODS = Space.format("\n\n    ");

    private AstFragments() {}

    public static Optional<J.MethodDeclaration> parseMethod(final String methodSource) {
        return firstMethodOf(HOLDER_PREFIX + methodSource + HOLDER_SUFFIX)
                .map(m -> m.withPrefix(BETWEEN_METHODS));
    }

    public static Optional<Statement> parseStatement(final String statementSource) {
        final String wrapper = HOLDER_PREFIX + "void __m__() { " + statementSource + " }" + HOLDER_SUFFIX;
        return firstMethodOf(wrapper)
                .filter(m -> m.getBody() != null && !m.getBody().getStatements().isEmpty())
                .map(m -> m.getBody().getStatements().getFirst());
    }

    /**
     * Parses a method body containing one or more statements and
     * returns every statement in order. Use when the caller needs to
     * splice a multi-statement block (e.g. an if-else chain plus a
     * trailing return) rather than a single statement. The body is
     * wrapped in {@code Object __m__() { <body> }} so declarations like
     * {@code Object result;} parse cleanly even inside a method that
     * is ultimately expected to return a value.
     */
    public static List<Statement> parseStatements(final String bodySource) {
        final String wrapper = HOLDER_PREFIX + "Object __m__() { " + bodySource + " }" + HOLDER_SUFFIX;
        return firstMethodOf(wrapper)
                .filter(m -> m.getBody() != null)
                .<List<Statement>>map(m -> List.copyOf(m.getBody().getStatements()))
                .orElseGet(List::of);
    }

    /**
     * Parses the first member of a holder class literal like
     * {@code "class _Tmp { private static final String X = \"…\"; }"}
     * and returns it as a {@link Statement}. Meant for recipes that
     * prepend a newly-synthesised field to an existing class body.
     */
    public static Optional<Statement> parseField(final String classSource) {
        return parseHolderClass(classSource)
                .filter(cls -> !cls.getBody().getStatements().isEmpty())
                .map(cls -> cls.getBody().getStatements().getFirst());
    }

    private static Optional<J.MethodDeclaration> firstMethodOf(final String source) {
        return parseHolderClass(source)
                .filter(cls -> !cls.getBody().getStatements().isEmpty()
                        && cls.getBody().getStatements().getFirst() instanceof J.MethodDeclaration)
                .map(cls -> (J.MethodDeclaration) cls.getBody().getStatements().getFirst());
    }

    private static Optional<J.ClassDeclaration> parseHolderClass(final String source) {
        final List<SourceFile> parsed = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build().parse(source).toList();
        if (parsed.isEmpty() || !(parsed.getFirst() instanceof J.CompilationUnit cu)) {
            return Optional.empty();
        }
        if (cu.getClasses().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(cu.getClasses().getFirst());
    }
}
