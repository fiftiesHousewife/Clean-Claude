package io.github.fiftieshousewife.cleancode.core;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FixBriefGenerator {

    private static final String UNASSIGNED = "(project-level findings with no source file)";

    private FixBriefGenerator() {}

    public static List<Path> generate(final AggregatedReport report, final Path outputDir) throws IOException {
        return generate(report, outputDir, null);
    }

    public static List<Path> generate(final AggregatedReport report, final Path outputDir,
                                      final Path projectRoot) throws IOException {
        Files.createDirectories(outputDir);

        final Map<String, List<Finding>> byFile = groupByFile(report.findings());
        final List<Path> written = new ArrayList<>();

        for (final Map.Entry<String, List<Finding>> entry : byFile.entrySet()) {
            final String fileName = briefFileName(entry.getKey());
            final Path brief = outputDir.resolve(fileName);
            Files.writeString(brief, renderBrief(entry.getKey(), entry.getValue(), projectRoot));
            written.add(brief);
        }

        final Path indexFile = outputDir.resolve("_INDEX.md");
        Files.writeString(indexFile, renderIndex(report.projectName(), byFile));
        written.add(indexFile);

        return written;
    }

    private static Map<String, List<Finding>> groupByFile(final List<Finding> findings) {
        final Map<String, List<Finding>> byFile = new LinkedHashMap<>();
        findings.stream()
                .sorted(Comparator.comparing(FixBriefGenerator::fileKey)
                        .thenComparingInt(Finding::startLine))
                .forEach(f -> byFile.computeIfAbsent(fileKey(f), k -> new ArrayList<>()).add(f));
        return byFile;
    }

    private static String fileKey(final Finding f) {
        return f.sourceFile() == null ? UNASSIGNED : f.sourceFile();
    }

    private static String briefFileName(final String sourceFile) {
        if (UNASSIGNED.equals(sourceFile)) {
            return "project-level-findings.md";
        }
        final String base = sourceFile.replaceAll(".*/", "").replace(".java", "");
        return base + ".md";
    }

    private static String renderBrief(final String sourceFile, final List<Finding> findings,
                                       final Path projectRoot) {
        final StringBuilder sb = new StringBuilder();
        sb.append("# Fix brief: ").append(sourceFile).append('\n');
        sb.append('\n');
        sb.append(findings.size()).append(" finding").append(findings.size() == 1 ? "" : "s")
                .append(" on this file.\n");
        sb.append('\n');

        final List<String> siblings = findSiblings(sourceFile, projectRoot);
        if (!siblings.isEmpty()) {
            sb.append("## Sibling types in this package\n");
            sb.append("Types that live alongside this file. Read them directly from disk only when a "
                    + "finding needs their detail; the names alone should answer most package-context "
                    + "questions.\n\n");
            for (final String sibling : siblings) {
                sb.append("- ").append(sibling).append('\n');
            }
            sb.append('\n');
        }

        sb.append("## Before you touch any code\n");
        sb.append("Your first tool calls MUST be Reads of every skill path cited in the sections below. ")
                .append("Do not call Edit or Write before every skill file has been read in full. ")
                .append("These skills contain the worked examples, false-positive patterns, and rewrite ")
                .append("templates you need to make the correct fix.\n");
        sb.append('\n');
        sb.append("## Rules\n");
        sb.append("- Address only findings on this file. Do not modify other files except to fix compilation.\n");
        sb.append("- Prefer deleting dead code to refactoring it.\n");
        sb.append("- Never degrade readability to satisfy a metric. If a fix would make the code harder to "
                + "read (e.g. cramming methods onto one line to pass a line-count check), leave the finding "
                + "and note it in your final summary.\n");
        sb.append("- Run `./gradlew :<module>:test` after your changes. Tests must pass.\n");

        if (triggersMetricSqueezingWarning(findings)) {
            sb.append("\n> **Do not metric-squeeze.** This brief contains both a size-based finding "
                    + "(Ch10.1 / G30) and a duplication finding (G5). Split by responsibility, not by LOC. "
                    + "If splitting creates near-duplicate helpers or copy-pasted guard clauses, you are "
                    + "making the code worse — leave the size finding and note the design tradeoff in your "
                    + "final summary.\n");
        }
        sb.append('\n');

        final Map<HeuristicCode, List<Finding>> byCode = new LinkedHashMap<>();
        findings.stream()
                .sorted(Comparator.comparing((Finding f) -> f.severity().ordinal())
                        .thenComparing(f -> f.code().name()))
                .forEach(f -> byCode.computeIfAbsent(f.code(), k -> new ArrayList<>()).add(f));

        for (final Map.Entry<HeuristicCode, List<Finding>> entry : byCode.entrySet()) {
            appendCodeSection(sb, entry.getKey(), entry.getValue());
        }

        sb.append("## Final self-check\n");
        sb.append("Before handing back, confirm:\n");
        sb.append("1. Tests pass for the affected module.\n");
        sb.append("2. Each change makes the code clearer, not just shorter.\n");
        sb.append("3. List any findings you intentionally did not fix and why.\n");
        return sb.toString();
    }

    private static List<String> findSiblings(final String sourceFile, final Path projectRoot) {
        if (projectRoot == null || sourceFile == null || !sourceFile.endsWith(".java")) {
            return List.of();
        }
        final Path absolute = projectRoot.resolve(sourceFile);
        final Path packageDir = absolute.getParent();
        if (packageDir == null || !Files.isDirectory(packageDir)) {
            return List.of();
        }
        final String thisFileName = absolute.getFileName().toString();
        try (var stream = Files.list(packageDir)) {
            return stream
                    .map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".java"))
                    .filter(n -> !n.equals(thisFileName))
                    .sorted()
                    .map(n -> n.substring(0, n.length() - ".java".length()))
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private static boolean triggersMetricSqueezingWarning(final List<Finding> findings) {
        boolean sawSize = false;
        boolean sawDuplication = false;
        for (final Finding f : findings) {
            if (f.code() == HeuristicCode.Ch10_1 || f.code() == HeuristicCode.G30) {
                sawSize = true;
            }
            if (f.code() == HeuristicCode.G5) {
                sawDuplication = true;
            }
        }
        return sawSize && sawDuplication;
    }

    private static void appendCodeSection(final StringBuilder sb, final HeuristicCode code,
                                          final List<Finding> findings) {
        final String title = HeuristicDescriptions.name(code);
        sb.append("## ").append(code.name()).append(": ").append(title).append('\n');
        sb.append('\n');
        final String skillPath = SkillPathRegistry.skillPathFor(code);
        if (skillPath != null) {
            sb.append("> **You MUST Read this file first — before any Edit or Write tool call:** `")
                    .append(skillPath).append("`\n\n");
        }
        if (code == HeuristicCode.E1) {
            sb.append("> Act on every E1 finding. Bump the version in `gradle/libs.versions.toml`, "
                    + "one commit per dep, and run `./gradlew test` before moving on. Only skip a "
                    + "bump when it is a major-version jump with a breaking changelog — document the "
                    + "skip and the changelog link in your final summary.\n\n");
        }
        for (final Finding f : findings) {
            sb.append("- ");
            if (f.startLine() > 0) {
                sb.append("L").append(f.startLine());
                if (f.endLine() > f.startLine()) {
                    sb.append('-').append(f.endLine());
                }
                sb.append(": ");
            }
            sb.append(f.message()).append('\n');
        }
        sb.append('\n');
    }

    private static String renderIndex(final String projectName,
                                      final Map<String, List<Finding>> byFile) {
        final StringBuilder sb = new StringBuilder();
        sb.append("# Fix briefs for ").append(projectName).append('\n');
        sb.append('\n');
        sb.append("One brief per file. Each brief lists every finding on that file and points at the "
                + "relevant skill. Designed to be handed to a single agent so two agents never edit the same "
                + "file.\n");
        sb.append('\n');
        sb.append("| File | Findings | Brief |\n");
        sb.append("|---|---:|---|\n");
        byFile.entrySet().stream()
                .sorted(Map.Entry.<String, List<Finding>>comparingByValue(
                        Comparator.comparingInt(List::size)).reversed())
                .forEach(e -> sb.append("| ").append(e.getKey())
                        .append(" | ").append(e.getValue().size())
                        .append(" | [").append(briefFileName(e.getKey())).append("](")
                        .append(briefFileName(e.getKey())).append(") |\n"));
        return sb.toString();
    }
}
