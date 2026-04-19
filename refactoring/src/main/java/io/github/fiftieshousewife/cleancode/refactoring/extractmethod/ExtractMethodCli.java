package io.github.fiftieshousewife.cleancode.refactoring.extractmethod;

import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Command-line driver so a human or an agent can run {@link ExtractMethodRecipe}
 * against a single source file without wiring a full OpenRewrite recipe
 * chain. Wired up by the {@code extractMethod} Gradle task in the
 * refactoring module's build file; see {@code docs/extract-method-recipe.md}
 * for the three usage modes.
 */
public final class ExtractMethodCli {

    private static final String FILE_PROPERTY = "extractmethod.file";
    private static final String START_LINE_PROPERTY = "extractmethod.startLine";
    private static final String END_LINE_PROPERTY = "extractmethod.endLine";
    private static final String NEW_METHOD_NAME_PROPERTY = "extractmethod.newMethodName";

    private ExtractMethodCli() {}

    public static void main(final String[] args) throws IOException {
        final Path path = Path.of(required(FILE_PROPERTY));
        final int startLine = Integer.parseInt(required(START_LINE_PROPERTY));
        final int endLine = Integer.parseInt(required(END_LINE_PROPERTY));
        final String newMethodName = required(NEW_METHOD_NAME_PROPERTY);

        final String source = Files.readString(path);
        final ExtractMethodRecipe recipe = new ExtractMethodRecipe(
                path.getFileName().toString(), startLine, endLine, newMethodName);
        final List<SourceFile> parsed = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build().parse(source).toList();
        final InMemoryExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        final List<Result> changed = recipe.run(new InMemoryLargeSourceSet(parsed), ctx)
                .getChangeset().getAllResults();
        if (changed.isEmpty()) {
            System.out.println("no extraction performed — recipe rejected or range not found");
            return;
        }
        Files.writeString(path, changed.getFirst().getAfter().printAll());
        System.out.println("extracted " + newMethodName + " from lines "
                + startLine + "-" + endLine + " of " + path);
    }

    private static String required(final String key) {
        final String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing system property: -D" + key);
        }
        return value;
    }
}
