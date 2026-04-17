package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class BriefMarkdown {

    private BriefMarkdown() {}

    static String render(final String sourceFile, final List<Finding> findings, final Path projectRoot) {
        final StringBuilder sb = new StringBuilder();
        appendHeader(sb, sourceFile, findings);
        appendSiblings(sb, PackageSiblings.findFor(sourceFile, projectRoot));
        appendPreamble(sb, findings);
        appendCodeSections(sb, findings);
        appendSelfCheck(sb);
        return sb.toString();
    }

    private static void appendHeader(final StringBuilder sb, final String sourceFile, final List<Finding> findings) {
        sb.append("# Fix brief: ").append(sourceFile).append('\n');
        sb.append('\n');
        sb.append(findings.size()).append(" finding").append(findings.size() == 1 ? "" : "s")
                .append(" on this file.\n");
        sb.append('\n');
    }

    private static void appendSiblings(final StringBuilder sb, final List<String> siblings) {
        if (siblings.isEmpty()) {
            return;
        }
        sb.append("## Sibling types in this package\n");
        sb.append("Types that live alongside this file. Read them directly from disk only when a "
                + "finding needs their detail; the names alone should answer most package-context "
                + "questions.\n\n");
        siblings.forEach(sibling -> sb.append("- ").append(sibling).append('\n'));
        sb.append('\n');
    }

    private static void appendPreamble(final StringBuilder sb, final List<Finding> findings) {
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
    }

    private static void appendCodeSections(final StringBuilder sb, final List<Finding> findings) {
        final Map<HeuristicCode, List<Finding>> byCode = new LinkedHashMap<>();
        findings.stream()
                .sorted(Comparator.comparing((Finding f) -> f.severity().ordinal())
                        .thenComparing(f -> f.code().name()))
                .forEach(f -> byCode.computeIfAbsent(f.code(), k -> new ArrayList<>()).add(f));

        byCode.forEach((code, codeFindings) -> appendCodeSection(sb, code, codeFindings));
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
        findings.forEach(f -> appendFindingLine(sb, f));
        sb.append('\n');
    }

    private static void appendFindingLine(final StringBuilder sb, final Finding f) {
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

    private static void appendSelfCheck(final StringBuilder sb) {
        sb.append("## Final self-check\n");
        sb.append("Before handing back, confirm:\n");
        sb.append("1. Tests pass for the affected module.\n");
        sb.append("2. Each change makes the code clearer, not just shorter.\n");
        sb.append("3. List any findings you intentionally did not fix and why.\n");
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
}
