package io.github.fiftieshousewife.cleancode.core;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public final class ClaudeMdGenerator {

    private static final List<HeuristicCode> NARRATIVE_STUB_CODES = List.of(
            HeuristicCode.G2, HeuristicCode.G3, HeuristicCode.G6, HeuristicCode.G13,
            HeuristicCode.G20, HeuristicCode.G21, HeuristicCode.G31, HeuristicCode.G32
    );

    private ClaudeMdGenerator() {}

    public static void generate(final AggregatedReport report, final Path claudeMdFile,
                                final Path baselineFile) throws IOException {
        generate(report, claudeMdFile, baselineFile, List.of());
    }

    public static void generate(final AggregatedReport report, final Path claudeMdFile,
                                final Path baselineFile,
                                final List<String> dependencies) throws IOException {
        final Map<String, String> preservedAnnotations = new LinkedHashMap<>();
        if (Files.exists(claudeMdFile)) {
            preservedAnnotations.putAll(parseAnnotateSections(Files.readString(claudeMdFile)));
        }

        final StringBuilder sb = new StringBuilder();

        appendPreamble(sb);
        appendFrameworksSection(sb, dependencies);
        appendDeltaTable(sb, report, baselineFile);
        appendFindingSections(sb, report, preservedAnnotations);
        appendNarrativeStubs(sb, report, preservedAnnotations);

        Files.writeString(claudeMdFile, sb.toString());
    }

    private static void appendPreamble(final StringBuilder sb) {
        sb.append("## Before you start any work in this codebase\n\n");
        sb.append("1. Read `.claude/skills/SKILLS.md` now, before reading anything else.\n");
        sb.append("   This is mandatory, not optional.\n");
        sb.append("2. When a finding section below points to a skill file, read it before\n");
        sb.append("   acting on that finding.\n");
        sb.append("3. When working on anything not covered by a finding, check SKILLS.md\n");
        sb.append("   for a matching skill before proceeding.\n\n");
    }

    private static void appendFrameworksSection(final StringBuilder sb, final List<String> dependencies) {
        final List<String> frameworks = FrameworkRegistry.detect(dependencies);
        if (frameworks.isEmpty()) {
            return;
        }
        sb.append("## Frameworks in use\n\n");
        frameworks.forEach(f -> sb.append(String.format("- %s%n", f)));
        sb.append('\n');
    }

    private static void appendNarrativeStubs(final StringBuilder sb, final AggregatedReport report,
                                              final Map<String, String> preservedAnnotations) {
        final Set<HeuristicCode> codesWithFindings = report.byCode().keySet();
        NARRATIVE_STUB_CODES.stream()
                .filter(code -> !codesWithFindings.contains(code))
                .forEach(code -> {
                    final String codeName = code.name();
                    final String heading = HeuristicDescriptions.name(code);
                    sb.append(String.format("## %s: %s%n", codeName, heading));
                    sb.append("> This heuristic requires human judgment"
                            + " \u2014 no automated detection available.\n\n");
                    sb.append(String.format("<!-- ANNOTATE: %s -->%n", codeName));
                    if (preservedAnnotations.containsKey(codeName)) {
                        sb.append(preservedAnnotations.get(codeName));
                    } else {
                        sb.append("<!-- Add project-specific guidance for this heuristic here -->\n");
                    }
                    sb.append("<!-- /ANNOTATE -->\n\n");
                });
    }

    private static void appendDeltaTable(final StringBuilder sb, final AggregatedReport report,
                                          final Path baselineFile) throws IOException {
        if (baselineFile == null || !Files.exists(baselineFile)) {
            return;
        }

        final String json = Files.readString(baselineFile);
        final Gson gson = new Gson();
        final Map<String, Object> raw = gson.fromJson(json, new TypeToken<Map<String, Object>>() {}.getType());
        @SuppressWarnings("unchecked")
        final Map<String, Double> baselineCounts = (Map<String, Double>) raw.get("counts");
        if (baselineCounts == null) {
            return;
        }

        final Map<HeuristicCode, Long> currentCounts = report.findings().stream()
                .collect(Collectors.groupingBy(Finding::code, Collectors.counting()));

        final Set<String> allCodes = new TreeSet<>();
        allCodes.addAll(baselineCounts.keySet());
        currentCounts.keySet().forEach(c -> allCodes.add(c.name()));

        sb.append("## Current standing vs baseline\n\n");
        sb.append("| Category | Baseline | Current | Delta |\n");
        sb.append("|---|---|---|---|\n");

        for (final String codeName : allCodes) {
            final int baseline = baselineCounts.containsKey(codeName)
                    ? baselineCounts.get(codeName).intValue() : 0;
            long current = 0;
            try {
                current = currentCounts.getOrDefault(HeuristicCode.valueOf(codeName), 0L);
            } catch (final IllegalArgumentException ignored) {
                // Unknown code in baseline — not a code we recognise
            }
            final long delta = current - baseline;
            final String deltaStr = delta == 0 ? "0"
                    : (delta > 0 ? "+" + delta + " \u26A0" : delta + " \u2713");
            sb.append(String.format("| %s | %d | %d | %s |%n", codeName, baseline, current, deltaStr));
        }
        sb.append('\n');
    }

    private static void appendFindingSections(final StringBuilder sb, final AggregatedReport report,
                                               final Map<String, String> preservedAnnotations) {
        final Map<HeuristicCode, List<Finding>> byCode = report.byCode();

        for (final Map.Entry<HeuristicCode, List<Finding>> entry : byCode.entrySet()) {
            final HeuristicCode code = entry.getKey();
            final List<Finding> findings = entry.getValue();

            sb.append(String.format("## %s [%d finding%s]%n", code.name(),
                    findings.size(), findings.size() == 1 ? "" : "s"));

            final String skillPath = SkillPathRegistry.skillPathFor(code);
            if (skillPath != null) {
                sb.append(String.format("> Read `%s` before addressing these.%n%n", skillPath));
            } else {
                sb.append('\n');
            }

            final String annotateKey = code.name();
            if (preservedAnnotations.containsKey(annotateKey)) {
                sb.append(String.format("<!-- ANNOTATE: %s -->%n", annotateKey));
                sb.append(preservedAnnotations.get(annotateKey));
                sb.append(String.format("<!-- /ANNOTATE -->%n%n"));
            }

            sb.append(String.format("<!-- GENERATED: %s -->%n", code.name()));
            sb.append("**From analysis:**\n");
            for (final Finding f : findings) {
                if (f.sourceFile() != null) {
                    sb.append(String.format("- %s:%d%n", f.sourceFile(), f.startLine()));
                } else {
                    sb.append(String.format("- (project-level) %s%n", f.message()));
                }
            }
            sb.append("<!-- /GENERATED -->\n\n");
        }
    }

    private static Map<String, String> parseAnnotateSections(final String content) {
        final Map<String, String> sections = new LinkedHashMap<>();
        final List<String> lines = content.lines().toList();

        int i = 0;
        while (i < lines.size()) {
            String line = lines.get(i);
            if (line.startsWith("<!-- ANNOTATE:")) {
                String code = line.substring("<!-- ANNOTATE:".length()).replace("-->", "").trim();
                StringBuilder body = new StringBuilder();
                i++;
                while (i < lines.size() && !lines.get(i).startsWith("<!-- /ANNOTATE")) {
                    body.append(lines.get(i)).append('\n');
                    i++;
                }
                sections.put(code, body.toString());
            }
            i++;
        }
        return sections;
    }
}
