package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

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
        Files.createDirectories(outputDir);
        final Map<String, List<Finding>> byFile = groupByFile(report.findings());
        final List<Path> written = writeBriefs(byFile, outputDir);
        written.add(writeIndex(report.projectName(), byFile, outputDir));
        return written;
    }

    private static List<Path> writeBriefs(final Map<String, List<Finding>> byFile,
                                          final Path outputDir) throws IOException {
        final List<Path> written = new ArrayList<>();
        for (final Map.Entry<String, List<Finding>> entry : byFile.entrySet()) {
            final Path brief = outputDir.resolve(briefFileName(entry.getKey()));
            Files.writeString(brief, renderBrief(entry.getKey(), entry.getValue()));
            written.add(brief);
        }
        return written;
    }

    private static Path writeIndex(final String projectName,
                                   final Map<String, List<Finding>> byFile,
                                   final Path outputDir) throws IOException {
        final Path indexFile = outputDir.resolve("_INDEX.md");
        Files.writeString(indexFile, renderIndex(projectName, byFile));
        return indexFile;
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
            return "_project-level.md";
        }
        final String base = sourceFile.replaceAll(".*/", "").replace(".java", "");
        return base + ".md";
    }

    static String renderBrief(final String sourceFile, final List<Finding> findings) {
        final StringBuilder sb = new StringBuilder();
        sb.append("# Fix brief: ").append(sourceFile).append('\n')
                .append('\n')
                .append(findings.size()).append(" finding").append(findings.size() == 1 ? "" : "s")
                .append(" on this file.\n")
                .append('\n')
                .append("## Rules\n")
                .append("- Address only findings on this file. Do not modify other files except to fix "
                        + "compilation.\n")
                .append("- Prefer deleting dead code to refactoring it.\n")
                .append("- Never degrade readability to satisfy a metric. If a fix would make the code harder "
                        + "to read (e.g. cramming methods onto one line to pass a line-count check), leave the "
                        + "finding and note it in your final summary.\n")
                .append("- Run `./gradlew :<module>:test` after your changes. Tests must pass.\n")
                .append('\n');
        for (final Map.Entry<HeuristicCode, List<Finding>> entry : groupByCode(findings).entrySet()) {
            sb.append(renderCodeSection(entry.getKey(), entry.getValue()));
        }
        sb.append("## Final self-check\n")
                .append("Before handing back, confirm:\n")
                .append("1. Tests pass for the affected module.\n")
                .append("2. Each change makes the code clearer, not just shorter.\n")
                .append("3. List any findings you intentionally did not fix and why.\n");
        return sb.toString();
    }

    private static Map<HeuristicCode, List<Finding>> groupByCode(final List<Finding> findings) {
        final Map<HeuristicCode, List<Finding>> byCode = new LinkedHashMap<>();
        findings.stream()
                .sorted(Comparator.comparing((Finding f) -> f.severity().ordinal())
                        .thenComparing(f -> f.code().name()))
                .forEach(f -> byCode.computeIfAbsent(f.code(), k -> new ArrayList<>()).add(f));
        return byCode;
    }

    static String renderCodeSection(final HeuristicCode code, final List<Finding> findings) {
        final StringBuilder sb = new StringBuilder();
        final String title = HeuristicDescriptions.name(code);
        sb.append("## ").append(code.name()).append(": ").append(title).append('\n')
                .append('\n');
        final String skillPath = SkillPathRegistry.skillPathFor(code);
        if (skillPath != null) {
            sb.append("> Read `").append(skillPath).append("` before addressing these.\n\n");
        }
        for (final Finding f : findings) {
            sb.append(renderFindingLine(f));
        }
        sb.append('\n');
        return sb.toString();
    }

    private static String renderFindingLine(final Finding f) {
        final StringBuilder sb = new StringBuilder("- ");
        if (f.startLine() > 0) {
            sb.append("L").append(f.startLine());
            if (f.endLine() > f.startLine()) {
                sb.append('-').append(f.endLine());
            }
            sb.append(": ");
        }
        sb.append(f.message()).append('\n');
        return sb.toString();
    }

    static String renderIndex(final String projectName,
                              final Map<String, List<Finding>> byFile) {
        final StringBuilder sb = new StringBuilder();
        sb.append("# Fix briefs for ").append(projectName).append('\n')
                .append('\n')
                .append("One brief per file. Each brief lists every finding on that file and points at the "
                        + "relevant skill. Designed to be handed to a single agent so two agents never edit "
                        + "the same file.\n")
                .append('\n')
                .append("| File | Findings | Brief |\n")
                .append("|---|---:|---|\n");
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
