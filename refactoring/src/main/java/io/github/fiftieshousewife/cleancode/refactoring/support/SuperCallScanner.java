package io.github.fiftieshousewife.cleancode.refactoring.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regex-scans Java source for {@code super.methodName(} calls and
 * returns the aggregate set of simple method names found. Used as a
 * lightweight pre-pass so {@code MakeMethodStaticRecipe} can protect
 * methods whose super-call site lives in a different compilation unit
 * from the method declaration.
 *
 * <p>The scan is deliberately coarse: it catches false positives (e.g.
 * a {@code super.foo} reference inside a string literal or comment)
 * because erring on the side of skipping a static-eligible method is
 * always safe. A full AST parse would eliminate the false positives but
 * is ~50x slower; for the sweep's pre-pass, regex is the right trade.
 */
public final class SuperCallScanner {

    private static final Pattern SUPER_CALL = Pattern.compile(
            "\\bsuper\\s*\\.\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*\\(");

    private SuperCallScanner() {}

    public static Set<String> scan(final Collection<Path> javaFiles) throws IOException {
        final Set<String> names = new HashSet<>();
        for (final Path file : javaFiles) {
            collectFrom(Files.readString(file), names);
        }
        return names;
    }

    public static Set<String> scanSource(final String source) {
        final Set<String> names = new HashSet<>();
        collectFrom(source, names);
        return names;
    }

    private static void collectFrom(final String source, final Set<String> collected) {
        final Matcher matcher = SUPER_CALL.matcher(source);
        while (matcher.find()) {
            collected.add(matcher.group(1));
        }
    }
}
