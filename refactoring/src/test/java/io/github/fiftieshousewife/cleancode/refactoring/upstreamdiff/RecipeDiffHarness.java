package io.github.fiftieshousewife.cleancode.refactoring.upstreamdiff;

import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Runs an "ours" recipe and an "upstream" recipe over the same Java
 * fixtures and classifies each fixture into one of: both no-op, both
 * fired with identical output, both fired with divergent output, only
 * ours fired, only upstream fired.
 *
 * <p>Used to evaluate whether one of this project's bespoke recipes can
 * be safely deleted in favour of an upstream OpenRewrite recipe. The
 * critical bucket is "divergent" — those are fixtures where the two
 * recipes disagree and a human must adjudicate.
 *
 * <p>Modelled directly on
 * {@code HarnessRecipePass.runOne} — same {@code Parser.Input.fromString}
 * trick to give the parser a real path so error messages are useful, and
 * the same source-path filter to ignore sibling Result entries (some
 * recipes emit fresh files like {@code log4j2.xml}).
 */
public final class RecipeDiffHarness {

    private RecipeDiffHarness() {}

    public record NamedSource(String name, String source) {
        public static NamedSource fromFile(final Path file) throws IOException {
            return new NamedSource(file.toString(), Files.readString(file));
        }
    }

    public record Divergence(String name, String original, String oursOutput, String upstreamOutput) {}

    public record DiffReport(
            int total,
            int bothNoOp,
            List<String> equivalent,
            List<Divergence> divergent,
            List<String> onlyOurs,
            List<String> onlyUpstream) {

        public boolean clean() {
            return divergent.isEmpty() && onlyOurs.isEmpty() && onlyUpstream.isEmpty();
        }

        public String summary() {
            return String.format(
                    "total=%d, bothNoOp=%d, equivalent=%d, divergent=%d, onlyOurs=%d, onlyUpstream=%d",
                    total, bothNoOp, equivalent.size(), divergent.size(),
                    onlyOurs.size(), onlyUpstream.size());
        }
    }

    public static Recipe loadUpstream(final String fullyQualifiedRecipeName) {
        return Environment.builder()
                .scanRuntimeClasspath()
                .build()
                .activateRecipes(fullyQualifiedRecipeName);
    }

    public static Recipe loadUpstream(final String first, final String... rest) {
        final String[] all = new String[rest.length + 1];
        all[0] = first;
        System.arraycopy(rest, 0, all, 1, rest.length);
        return Environment.builder()
                .scanRuntimeClasspath()
                .build()
                .activateRecipes(all);
    }

    public static DiffReport compare(
            final Recipe ours,
            final Recipe upstream,
            final Stream<NamedSource> corpus) {
        final List<String> equivalent = new ArrayList<>();
        final List<Divergence> divergent = new ArrayList<>();
        final List<String> onlyOurs = new ArrayList<>();
        final List<String> onlyUpstream = new ArrayList<>();
        int total = 0;
        int bothNoOp = 0;
        final List<NamedSource> materialised = corpus.toList();
        for (final NamedSource input : materialised) {
            total += 1;
            final String oursOut = run(ours, input);
            final String upstreamOut = run(upstream, input);
            final boolean oursFired = !oursOut.equals(input.source());
            final boolean upstreamFired = !upstreamOut.equals(input.source());
            if (!oursFired && !upstreamFired) {
                bothNoOp += 1;
            } else if (oursFired && upstreamFired) {
                if (oursOut.equals(upstreamOut)) {
                    equivalent.add(input.name());
                } else {
                    divergent.add(new Divergence(input.name(), input.source(), oursOut, upstreamOut));
                }
            } else if (oursFired) {
                onlyOurs.add(input.name());
            } else {
                onlyUpstream.add(input.name());
            }
        }
        return new DiffReport(total, bothNoOp, equivalent, divergent, onlyOurs, onlyUpstream);
    }

    private static String run(final Recipe recipe, final NamedSource input) {
        final InMemoryExecutionContext ctx = new InMemoryExecutionContext(t -> {});
        final Path syntheticPath = Path.of(input.name());
        final Parser.Input parserInput = Parser.Input.fromString(syntheticPath, input.source());
        final List<SourceFile> parsed = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build()
                .parseInputs(List.of(parserInput), null, ctx)
                .toList();
        if (parsed.isEmpty()) {
            return input.source();
        }
        final InMemoryLargeSourceSet sourceSet = new InMemoryLargeSourceSet(parsed);
        final List<Result> results = recipe.run(sourceSet, ctx).getChangeset().getAllResults();
        for (final Result result : results) {
            final SourceFile after = result.getAfter();
            if (after == null) {
                continue;
            }
            if (after.getSourcePath().equals(syntheticPath)
                    || after.getSourcePath().endsWith(syntheticPath.getFileName())) {
                return after.printAll();
            }
        }
        return input.source();
    }
}