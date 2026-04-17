package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ClaudeMdSectionBuilder {

    private static final List<HeuristicCode> NARRATIVE_STUB_CODES = List.of(
            HeuristicCode.G2, HeuristicCode.G3, HeuristicCode.G6, HeuristicCode.G13,
            HeuristicCode.G20, HeuristicCode.G21, HeuristicCode.G31, HeuristicCode.G32
    );

    private final AggregatedReport report;
    private final Map<String, String> preservedAnnotations;

    ClaudeMdSectionBuilder(final AggregatedReport report, final Map<String, String> preservedAnnotations) {
        this.report = report;
        this.preservedAnnotations = preservedAnnotations;
    }

    String build(final List<String> dependencies, final Path baselineFile) throws IOException {
        final StringBuilder withPreamble = appendPreamble(new StringBuilder());
        final StringBuilder withFrameworks = appendFrameworksSection(withPreamble, dependencies);
        final StringBuilder withBaseline = appendBaselineDelta(withFrameworks, baselineFile);
        final StringBuilder withFindings = appendFindingSections(withBaseline);
        final StringBuilder withNarrativeStubs = appendNarrativeStubs(withFindings);
        return withNarrativeStubs.toString();
    }

    StringBuilder appendPreamble(final StringBuilder sb) {
        sb.append("## Before you start any work in this codebase\n\n");
        sb.append("1. Read `.claude/skills/SKILLS.md` now, before reading anything else.\n");
        sb.append("   This is mandatory, not optional.\n");
        sb.append("2. When a finding section below points to a skill file, read it before\n");
        sb.append("   acting on that finding.\n");
        sb.append("3. When working on anything not covered by a finding, check SKILLS.md\n");
        sb.append("   for a matching skill before proceeding.\n\n");
        return sb;
    }

    private StringBuilder appendFrameworksSection(final StringBuilder sb, final List<String> dependencies) {
        final List<String> frameworks = FrameworkRegistry.detect(dependencies);
        if (frameworks.isEmpty()) {
            return sb;
        }
        sb.append("## Frameworks in use\n\n");
        frameworks.forEach(f -> sb.append(String.format("- %s%n", f)));
        sb.append('\n');
        return sb;
    }

    private StringBuilder appendBaselineDelta(final StringBuilder sb, final Path baselineFile) throws IOException {
        new BaselineDeltaTable(report).appendTo(sb, baselineFile);
        return sb;
    }

    private StringBuilder appendNarrativeStubs(final StringBuilder sb) {
        final Set<HeuristicCode> codesWithFindings = report.byCode().keySet();
        NARRATIVE_STUB_CODES.stream()
                .filter(code -> !codesWithFindings.contains(code))
                .forEach(code -> appendNarrativeStub(sb, code));
        return sb;
    }

    StringBuilder appendNarrativeStub(final StringBuilder sb, final HeuristicCode code) {
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
        return sb;
    }

    private StringBuilder appendFindingSections(final StringBuilder sb) {
        report.byCode().forEach((code, findings) -> appendFindingSection(sb, code, findings));
        return sb;
    }

    StringBuilder appendFindingSection(final StringBuilder sb, final HeuristicCode code,
                                       final List<Finding> findings) {
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
        findings.forEach(f -> appendFindingLine(sb, f));
        sb.append("<!-- /GENERATED -->\n\n");
        return sb;
    }

    private void appendFindingLine(final StringBuilder sb, final Finding f) {
        if (f.sourceFile() != null) {
            sb.append(String.format("- %s:%d%n", f.sourceFile(), f.startLine()));
        } else {
            sb.append(String.format("- (project-level) %s%n", f.message()));
        }
    }
}
