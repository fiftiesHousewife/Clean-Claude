package io.github.fiftieshousewife.cleancode.plugin.rework;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;
import io.github.fiftieshousewife.cleancode.core.Confidence;
import io.github.fiftieshousewife.cleancode.core.Finding;
import io.github.fiftieshousewife.cleancode.core.Severity;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FindingsSnapshotTest {

    private static final Path SANDBOX = Path.of("/proj/sandbox");
    private static final Path TARGET_A = SANDBOX.resolve("src/main/java/org/demo/A.java");
    private static final Path TARGET_B = SANDBOX.resolve("src/main/java/org/demo/B.java");
    private static final Path OUTSIDE = SANDBOX.resolve("src/main/java/org/demo/C.java");

    @Test
    void countsFixedIntroducedAndFinalFilteredToTargets() {
        final List<Finding> baseline = List.of(
                finding(HeuristicCode.G18, TARGET_A.toString(), 10),
                finding(HeuristicCode.G29, TARGET_A.toString(), 20),
                finding(HeuristicCode.G5, TARGET_B.toString(), 5),
                finding(HeuristicCode.G18, OUTSIDE.toString(), 7));
        final List<Finding> after = List.of(
                finding(HeuristicCode.G29, TARGET_A.toString(), 20),
                finding(HeuristicCode.G10, TARGET_A.toString(), 33),
                finding(HeuristicCode.G18, OUTSIDE.toString(), 7));

        final FindingsSnapshot snapshot = FindingsSnapshot.compute(
                baseline, after, Set.of(TARGET_A, TARGET_B), SANDBOX);

        assertAll(
                () -> assertEquals(3, snapshot.baseline()),
                () -> assertEquals(2, snapshot.fixed()),
                () -> assertEquals(1, snapshot.introduced()),
                () -> assertEquals(2, snapshot.finalCount()));
    }

    @Test
    void normalisesRelativeSourcePathsAgainstSandboxDir() {
        final String relative = "src/main/java/org/demo/A.java";
        final List<Finding> baseline = List.of(finding(HeuristicCode.G18, relative, 10));
        final List<Finding> after = List.of();

        final FindingsSnapshot snapshot = FindingsSnapshot.compute(
                baseline, after, Set.of(TARGET_A), SANDBOX);

        assertAll(
                () -> assertEquals(1, snapshot.baseline()),
                () -> assertEquals(1, snapshot.fixed()),
                () -> assertEquals(0, snapshot.introduced()),
                () -> assertEquals(0, snapshot.finalCount()));
    }

    @Test
    void shiftingFindingLineCountsAsIntroducedNotPreserved() {
        final List<Finding> baseline = List.of(finding(HeuristicCode.G29, TARGET_A.toString(), 10));
        final List<Finding> after = List.of(finding(HeuristicCode.G29, TARGET_A.toString(), 12));

        final FindingsSnapshot snapshot = FindingsSnapshot.compute(
                baseline, after, Set.of(TARGET_A), SANDBOX);

        assertAll(
                () -> assertEquals(1, snapshot.fixed()),
                () -> assertEquals(1, snapshot.introduced()),
                () -> assertEquals(1, snapshot.finalCount()));
    }

    @Test
    void ignoresProjectLevelFindingsWithNoSourceFile() {
        final Finding projectLevel = Finding.projectLevel(
                HeuristicCode.E1, "bump", Severity.WARNING, Confidence.HIGH, "tool", "rule");
        final FindingsSnapshot snapshot = FindingsSnapshot.compute(
                List.of(projectLevel), List.of(projectLevel), Set.of(TARGET_A), SANDBOX);

        assertEquals(new FindingsSnapshot(0, 0, 0, 0), snapshot);
    }

    private static Finding finding(final HeuristicCode code, final String sourceFile, final int line) {
        return Finding.at(code, sourceFile, line, line, "msg",
                Severity.WARNING, Confidence.HIGH, "tool", "rule");
    }
}
