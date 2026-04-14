package org.fiftieshousewife.cleancode.core;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public final class ClaudeMdGenerator {

    private ClaudeMdGenerator() {}

    public static void generate(AggregatedReport report, Path claudeMdFile, Path baselineFile) throws IOException {
        Map<String, String> preservedAnnotations = new LinkedHashMap<>();
        if (Files.exists(claudeMdFile)) {
            preservedAnnotations.putAll(parseAnnotateSections(Files.readString(claudeMdFile)));
        }

        StringBuilder sb = new StringBuilder();

        appendPreamble(sb);
        appendDeltaTable(sb, report, baselineFile);
        appendFindingSections(sb, report, preservedAnnotations);

        Files.writeString(claudeMdFile, sb.toString());
    }

    private static void appendPreamble(StringBuilder sb) {
        sb.append("## Before you start any work in this codebase\n\n");
        sb.append("1. Read `.claude/skills/SKILLS.md` now, before reading anything else.\n");
        sb.append("   This is mandatory, not optional.\n");
        sb.append("2. When a finding section below points to a skill file, read it before\n");
        sb.append("   acting on that finding.\n");
        sb.append("3. When working on anything not covered by a finding, check SKILLS.md\n");
        sb.append("   for a matching skill before proceeding.\n\n");
    }

    private static void appendDeltaTable(StringBuilder sb, AggregatedReport report, Path baselineFile) throws IOException {
        if (baselineFile == null || !Files.exists(baselineFile)) {
            return;
        }

        String json = Files.readString(baselineFile);
        Gson gson = new Gson();
        Map<String, Object> raw = gson.fromJson(json, new TypeToken<Map<String, Object>>() {}.getType());
        @SuppressWarnings("unchecked")
        Map<String, Double> baselineCounts = (Map<String, Double>) raw.get("counts");
        if (baselineCounts == null) {
            return;
        }

        Map<HeuristicCode, Long> currentCounts = report.findings().stream()
                .collect(Collectors.groupingBy(Finding::code, Collectors.counting()));

        Set<String> allCodes = new TreeSet<>();
        allCodes.addAll(baselineCounts.keySet());
        currentCounts.keySet().forEach(c -> allCodes.add(c.name()));

        sb.append("## Current standing vs baseline\n\n");
        sb.append("| Category | Baseline | Current | Delta |\n");
        sb.append("|---|---|---|---|\n");

        for (String codeName : allCodes) {
            int baseline = baselineCounts.containsKey(codeName)
                    ? baselineCounts.get(codeName).intValue() : 0;
            long current = 0;
            try {
                current = currentCounts.getOrDefault(HeuristicCode.valueOf(codeName), 0L);
            } catch (IllegalArgumentException ignored) {
                // Unknown code in baseline
            }
            long delta = current - baseline;
            String deltaStr = delta == 0 ? "0" : (delta > 0 ? "+" + delta + " \u26A0" : delta + " \u2713");
            sb.append(String.format("| %s | %d | %d | %s |%n", codeName, baseline, current, deltaStr));
        }
        sb.append('\n');
    }

    private static void appendFindingSections(StringBuilder sb, AggregatedReport report,
                                               Map<String, String> preservedAnnotations) {
        Map<HeuristicCode, List<Finding>> byCode = report.byCode();

        for (Map.Entry<HeuristicCode, List<Finding>> entry : byCode.entrySet()) {
            HeuristicCode code = entry.getKey();
            List<Finding> findings = entry.getValue();

            sb.append(String.format("## %s [%d finding%s]%n", code.name(),
                    findings.size(), findings.size() == 1 ? "" : "s"));

            String skillPath = SkillPathRegistry.skillPathFor(code);
            if (skillPath != null) {
                sb.append(String.format("> Read `%s` before addressing these.%n%n", skillPath));
            } else {
                sb.append('\n');
            }

            // Preserved ANNOTATE section
            String annotateKey = code.name();
            if (preservedAnnotations.containsKey(annotateKey)) {
                sb.append(String.format("<!-- ANNOTATE: %s -->%n", annotateKey));
                sb.append(preservedAnnotations.get(annotateKey));
                sb.append(String.format("<!-- /ANNOTATE -->%n%n"));
            }

            sb.append(String.format("<!-- GENERATED: %s -->%n", code.name()));
            sb.append("**From analysis:**\n");
            for (Finding f : findings) {
                if (f.sourceFile() != null) {
                    sb.append(String.format("- %s:%d%n", f.sourceFile(), f.startLine()));
                } else {
                    sb.append(String.format("- (project-level) %s%n", f.message()));
                }
            }
            sb.append("<!-- /GENERATED -->\n\n");
        }
    }

    private static Map<String, String> parseAnnotateSections(String content) {
        Map<String, String> sections = new LinkedHashMap<>();
        List<String> lines = content.lines().toList();

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
