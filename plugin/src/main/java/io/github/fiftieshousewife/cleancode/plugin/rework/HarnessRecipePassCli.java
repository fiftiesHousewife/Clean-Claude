package io.github.fiftieshousewife.cleancode.plugin.rework;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Throwaway driver: applies every deterministic recipe in
 * {@link HarnessRecipePass} to every non-excluded .java file under the
 * given root. Prints a per-file summary of which recipes fired. Meant
 * for one-off whole-codebase sweeps; not wired into the long-running
 * rework machinery.
 *
 * <p>Output goes through a {@link PrintStream} alias of {@code System.out}
 * rather than {@code System.out.println(...)} directly — the published
 * Lombok/Log4j recipe in the harness would otherwise rewrite these
 * calls into {@code log.info(...)} and add {@code @Log4j2}, breaking
 * compilation in a module without Lombok on the classpath.
 */
public final class HarnessRecipePassCli {

    private static final PrintStream OUT = System.out;

    // Sandbox holds deliberately-bad fixtures that the MCP tests verify
    // against fixed line numbers. Let the sweep skip them so whole-codebase
    // runs do not shift the fixtures' line numbers and break those tests.
    private static final Set<String> EXCLUDE_DIRS = Set.of(
            "build", ".gradle", ".git", "node_modules", "experiment",
            "build-logic", "docs", "gradle", "sandbox");

    private HarnessRecipePassCli() {}

    public static void main(final String[] args) throws IOException {
        final Path root = Path.of(args.length > 0 ? args[0] : ".").toAbsolutePath().normalize();
        final List<Path> files = collectJavaFiles(root);
        OUT.println("Found " + files.size() + " .java files under " + root);

        final HarnessRecipePass.PassSummary summary = HarnessRecipePass.apply(files);
        final Map<Path, List<String>> byFile = summary.recipeNamesByFile();

        OUT.println();
        OUT.println("=== Files changed: " + byFile.size() + " ===");
        byFile.forEach((file, recipes) -> {
            OUT.println(root.relativize(file));
            recipes.forEach(r -> OUT.println("  - " + r));
        });

        OUT.println();
        OUT.println("=== Totals ===");
        OUT.println("Files examined:  " + files.size());
        OUT.println("Files modified:  " + byFile.size());
        OUT.println("Recipe hits:     " + summary.allRecipeNames().size());
    }

    private static List<Path> collectJavaFiles(final Path root) throws IOException {
        final List<Path> files = new ArrayList<>();
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) {
                if (EXCLUDE_DIRS.contains(dir.getFileName().toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                if (file.toString().endsWith(".java")) {
                    files.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return files;
    }
}
