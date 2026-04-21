package io.github.fiftieshousewife.cleancode.plugin.rework;

import io.github.fiftieshousewife.cleancode.adapters.OpenRewriteFindingSource;
import io.github.fiftieshousewife.cleancode.core.Finding;
import io.github.fiftieshousewife.cleancode.core.ProjectContext;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Counts clean-code findings across every module in the project and
 * prints a before/after-friendly summary. Complements {@code sandbox}'s
 * {@code analyseCleanCode} self-analysis: the plugin is only applied to
 * sandbox, so we cannot ask Gradle for real finding counts in the other
 * modules. This CLI sidesteps the plugin by running
 * {@link OpenRewriteFindingSource} in-process per module, which is what
 * the recipe sweep actually targets anyway.
 *
 * <p>Output shape — stable so it can diff cleanly across runs:
 * <pre>
 *   === Whole-codebase OpenRewrite findings ===
 *   adapters     12
 *   annotations   0
 *   core          5
 *   ...
 *   ----
 *   Total       163
 *
 *   === By code ===
 *   G18   42
 *   G24   31
 *   ...
 * </pre>
 *
 * <p>Other finding sources (PMD, Checkstyle, SpotBugs, JaCoCo, CPD,
 * Ben-Manes) depend on pre-existing Gradle report files that only exist
 * in modules where the plugin has already run. Running them here would
 * return zero for non-sandbox modules and give a misleading baseline,
 * so only {@code OpenRewriteFindingSource} is invoked — it walks source
 * files directly.
 */
public final class WholeCodebaseSummaryCli {

    private static final PrintStream OUT = System.out;

    private static final Set<String> EXCLUDE_DIRS = Set.of(
            "build", ".gradle", ".git", "node_modules", "experiment",
            "build-logic", "docs", "gradle", "sandbox");

    private WholeCodebaseSummaryCli() {}

    public static void main(final String[] args) throws IOException {
        final Path root = Path.of(args.length > 0 ? args[0] : ".").toAbsolutePath().normalize();
        final List<Path> moduleMainSrcDirs = findModuleMainSourceRoots(root);
        OUT.println("Found " + moduleMainSrcDirs.size() + " module(s) with src/main/java under " + root);

        final OpenRewriteFindingSource source = new OpenRewriteFindingSource();
        final Map<String, Integer> perModule = new LinkedHashMap<>();
        final Map<String, Integer> perCode = new TreeMap<>();
        int total = 0;

        for (final Path mainSrc : moduleMainSrcDirs) {
            final Path moduleRoot = mainSrc.getParent().getParent().getParent();
            final String moduleName = root.relativize(moduleRoot).toString();
            final ProjectContext context = new ProjectContext(
                    moduleRoot,
                    moduleName.isEmpty() ? moduleRoot.getFileName().toString() : moduleName,
                    "1.0-SNAPSHOT",
                    "25",
                    List.of(mainSrc),
                    List.of(moduleRoot.resolve("src/test/java")),
                    moduleRoot.resolve("build"),
                    moduleRoot.resolve("build/reports"),
                    List.of());

            final List<Finding> findings = collectFindingsSafely(source, context);
            perModule.put(context.projectName(), findings.size());
            total += findings.size();
            findings.forEach(f -> perCode.merge(f.code().name(), 1, Integer::sum));
        }

        printTotals(perModule, total, perCode);
    }

    private static List<Finding> collectFindingsSafely(final OpenRewriteFindingSource source,
                                                       final ProjectContext context) {
        try {
            return source.collectFindings(context);
        } catch (Exception ex) {
            OUT.println("  [warn] " + context.projectName() + " failed: " + ex.getMessage());
            return List.of();
        }
    }

    private static void printTotals(final Map<String, Integer> perModule, final int total,
                                    final Map<String, Integer> perCode) {
        OUT.println();
        OUT.println("=== Whole-codebase OpenRewrite findings ===");
        final int nameWidth = perModule.keySet().stream().mapToInt(String::length).max().orElse(0);
        perModule.forEach((name, count) ->
                OUT.printf("%-" + Math.max(nameWidth, 10) + "s  %5d%n", name, count));
        OUT.println("----");
        OUT.printf("%-" + Math.max(nameWidth, 10) + "s  %5d%n", "Total", total);

        OUT.println();
        OUT.println("=== By code ===");
        perCode.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> OUT.printf("%-6s %5d%n", e.getKey(), e.getValue()));
    }

    private static List<Path> findModuleMainSourceRoots(final Path root) throws IOException {
        final List<Path> found = new ArrayList<>();
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) {
                if (!dir.equals(root) && EXCLUDE_DIRS.contains(dir.getFileName().toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                final Path mainSrc = dir.resolve("src/main/java");
                if (Files.isDirectory(mainSrc)) {
                    found.add(mainSrc);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        found.sort(Comparator.comparing(Path::toString));
        return found;
    }
}
