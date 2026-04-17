package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        final Map<String, String> preservedAnnotations = loadPreservedAnnotations(claudeMdFile);

        final String preamble = buildPreamble();
        final String frameworks = buildFrameworksSection(dependencies);
        final String deltaTable = buildDeltaTable(report, baselineFile);
        final String findingSections = buildFindingSections(report, preservedAnnotations);
        final String narrativeStubs = buildNarrativeStubs(report, preservedAnnotations);

        Files.writeString(claudeMdFile, preamble + frameworks + deltaTable + findingSections + narrativeStubs);
    }

    private static Map<String, String> loadPreservedAnnotations(final Path claudeMdFile) throws IOException {
        final Map<String, String> preservedAnnotations = new LinkedHashMap<>();
        if (Files.exists(claudeMdFile)) {
            preservedAnnotations.putAll(AnnotateSectionParser.parse(Files.readString(claudeMdFile)));
        }
        return preservedAnnotations;
    }

    static String buildPreamble() {
        return """
                ## Before you start any work in this codebase

                1. Read `.claude/skills/SKILLS.md` now, before reading anything else.
                   This is mandatory, not optional.
                2. When a finding section below points to a skill file, read it before
                   acting on that finding.
                3. When working on anything not covered by a finding, check SKILLS.md
                   for a matching skill before proceeding.

                """;
    }

    private static String buildFrameworksSection(final List<String> dependencies) {
        final List<String> frameworks = FrameworkRegistry.detect(dependencies);
        if (frameworks.isEmpty()) {
            return "";
        }
        final StringBuilder sb = new StringBuilder("## Frameworks in use\n\n");
        frameworks.forEach(f -> sb.append(String.format("- %s%n", f)));
        sb.append('\n');
        return sb.toString();
    }

    private static String buildDeltaTable(final AggregatedReport report, final Path baselineFile) throws IOException {
        final StringBuilder sb = new StringBuilder();
        DeltaTableWriter.append(sb, report, baselineFile);
        return sb.toString();
    }

    private static String buildNarrativeStubs(final AggregatedReport report,
                                              final Map<String, String> preservedAnnotations) {
        final Set<HeuristicCode> codesWithFindings = report.byCode().keySet();
        final StringBuilder sb = new StringBuilder();
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
        return sb.toString();
    }

    private static String buildFindingSections(final AggregatedReport report,
                                               final Map<String, String> preservedAnnotations) {
        final StringBuilder sb = new StringBuilder();
        report.byCode().forEach((code, findings) -> sb.append(buildFindingSection(code, findings, preservedAnnotations)));
        return sb.toString();
    }

    private static String buildFindingSection(final HeuristicCode code, final List<Finding> findings,
                                              final Map<String, String> preservedAnnotations) {
        return sectionHeading(code, findings)
                + skillPointerLine(code)
                + preservedAnnotationBlock(code, preservedAnnotations)
                + generatedFindingsBlock(code, findings);
    }

    private static String sectionHeading(final HeuristicCode code, final List<Finding> findings) {
        return String.format("## %s [%d finding%s]%n", code.name(),
                findings.size(), findings.size() == 1 ? "" : "s");
    }

    private static String skillPointerLine(final HeuristicCode code) {
        final String skillPath = SkillPathRegistry.skillPathFor(code);
        if (skillPath == null) {
            return "\n";
        }
        return String.format("> Read `%s` before addressing these.%n%n", skillPath);
    }

    private static String preservedAnnotationBlock(final HeuristicCode code,
                                                   final Map<String, String> preservedAnnotations) {
        final String annotateKey = code.name();
        if (!preservedAnnotations.containsKey(annotateKey)) {
            return "";
        }
        return String.format("<!-- ANNOTATE: %s -->%n", annotateKey)
                + preservedAnnotations.get(annotateKey)
                + String.format("<!-- /ANNOTATE -->%n%n");
    }

    private static String generatedFindingsBlock(final HeuristicCode code, final List<Finding> findings) {
        final StringBuilder sb = new StringBuilder();
        sb.append(String.format("<!-- GENERATED: %s -->%n", code.name()));
        sb.append("**From analysis:**\n");
        findings.forEach(f -> sb.append(formatFinding(f)));
        sb.append("<!-- /GENERATED -->\n\n");
        return sb.toString();
    }

    private static String formatFinding(final Finding finding) {
        if (finding.sourceFile() != null) {
            return String.format("- %s:%d%n", finding.sourceFile(), finding.startLine());
        }
        return String.format("- (project-level) %s%n", finding.message());
    }
}
