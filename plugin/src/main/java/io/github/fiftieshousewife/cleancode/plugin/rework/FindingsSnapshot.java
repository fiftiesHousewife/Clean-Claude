package io.github.fiftieshousewife.cleancode.plugin.rework;

import io.github.fiftieshousewife.cleancode.core.Finding;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Per-variant finding counts, scoped to the rework target files.
 * {@link ReworkCompareTask} computes one snapshot per variant: the
 * baseline findings come from the pre-run {@code findings.json}, the
 * current findings come from re-running analysis on the agent-edited
 * sandbox state before the files are restored. A finding is identified
 * by {@code (heuristicCode, sourceFile, startLine)} — same-line re-
 * emissions after an edit count as the same finding; shifted lines count
 * as a new one, which is the right call for a harness that's trying to
 * surface regressions rather than diff gymnastics.
 */
public record FindingsSnapshot(int baseline, int fixed, int introduced, int finalCount) {

    public static FindingsSnapshot compute(final List<Finding> baselineFindings,
                                           final List<Finding> currentFindings,
                                           final Set<Path> targetFiles,
                                           final Path sandboxProjectDir) {
        final Set<String> keysBefore = keysInTargets(baselineFindings, targetFiles, sandboxProjectDir);
        final Set<String> keysAfter = keysInTargets(currentFindings, targetFiles, sandboxProjectDir);
        final Set<String> stillPresent = new HashSet<>(keysBefore);
        stillPresent.retainAll(keysAfter);
        final int fixed = keysBefore.size() - stillPresent.size();
        final int introduced = keysAfter.size() - stillPresent.size();
        return new FindingsSnapshot(keysBefore.size(), fixed, introduced, keysAfter.size());
    }

    /**
     * Returns the findings from {@code currentFindings} that fall on one of
     * {@code targetFiles} and do NOT correspond (by code + file + startLine)
     * to any finding in {@code beforeFindings}. Used by the post-agent
     * feedback loop to hand the agent a focused "you introduced these" list.
     */
    public static List<Finding> introducedFindings(final List<Finding> beforeFindings,
                                                   final List<Finding> currentFindings,
                                                   final Set<Path> targetFiles,
                                                   final Path sandboxProjectDir) {
        final Set<String> beforeKeys = keysInTargets(beforeFindings, targetFiles, sandboxProjectDir);
        final java.util.List<Finding> introduced = new java.util.ArrayList<>();
        for (final Finding finding : currentFindings) {
            if (finding.sourceFile() == null) {
                continue;
            }
            final Path absolute = absolutise(finding.sourceFile(), sandboxProjectDir);
            if (!targetFiles.contains(absolute)) {
                continue;
            }
            final String key = finding.code().name() + '\u0000' + absolute + '\u0000' + finding.startLine();
            if (!beforeKeys.contains(key)) {
                introduced.add(finding);
            }
        }
        return introduced;
    }

    private static Set<String> keysInTargets(final List<Finding> findings,
                                             final Set<Path> targetFiles,
                                             final Path sandboxProjectDir) {
        final Set<String> keys = new HashSet<>();
        for (final Finding finding : findings) {
            if (finding.sourceFile() == null) {
                continue;
            }
            final Path absolute = absolutise(finding.sourceFile(), sandboxProjectDir);
            if (!targetFiles.contains(absolute)) {
                continue;
            }
            keys.add(finding.code().name() + '\u0000'
                    + absolute + '\u0000'
                    + finding.startLine());
        }
        return keys;
    }

    private static Path absolutise(final String sourceFile, final Path sandboxProjectDir) {
        final Path path = Path.of(sourceFile);
        return path.isAbsolute() ? path.normalize() : sandboxProjectDir.resolve(path).normalize();
    }
}
