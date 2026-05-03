package io.github.fiftieshousewife.cleancode.adapters;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;
import io.github.fiftieshousewife.cleancode.core.Finding;
import io.github.fiftieshousewife.cleancode.core.FindingSourceException;
import io.github.fiftieshousewife.cleancode.core.ProjectContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Verifies the line each finding reports actually corresponds to the
 * construct the heuristic is about. Uses verbatim copies of real
 * classes from the plugin module — synthetic fixtures hide drift caused
 * by Javadoc, lambdas, multiline annotations, switch yield, and other
 * real-world AST shapes.
 *
 * <p>For each finding produced from a fixture, we look at the line of
 * source it points at and check that line matches the heuristic's
 * expected anchor pattern (e.g., a Ch7_1 finding's line must contain
 * the {@code catch} keyword, a G28 line must contain a conditional
 * operator). Findings whose line text doesn't match are collected into
 * a drift report; the test fails with that report attached so each
 * miss is visible at once.
 *
 * <p>This is intentionally a forgiving floor (we accept the line OR
 * the few lines around it because the recipe might anchor at the
 * statement vs the keyword). What it guards against is: "the snippet
 * shows Javadoc / unrelated method / class declaration" — which is
 * the user-visible failure mode.
 */
class LineNumberAccuracyIntegrationTest {

    /** A finding that pointed at a line that doesn't match the heuristic's anchor pattern. */
    private record Drift(String fixture, HeuristicCode code, int line, String lineText, String message) {
        @Override
        public String toString() {
            return "%s:%d %s — line is %s — message: %s".formatted(
                    fixture, line, code, snippet(lineText), message);
        }

        private static String snippet(final String lineText) {
            if (lineText == null) {
                return "(out of range)";
            }
            final String trimmed = lineText.strip();
            return trimmed.length() > 80 ? "\"" + trimmed.substring(0, 77) + "...\"" : "\"" + trimmed + "\"";
        }
    }

    /**
     * Per-heuristic anchor expectations. Each entry says: when the
     * heuristic's finding lands on line N, the text of line N (or one
     * of the surrounding {@code TOLERANCE} lines) must match the
     * supplied predicate. If neither matches, we record a drift.
     */
    private static final int TOLERANCE = 1;

    private static final Map<HeuristicCode, Pattern> ANCHOR_PATTERNS = new LinkedHashMap<>(Map.of(
            HeuristicCode.Ch7_1, Pattern.compile("\\bcatch\\s*\\("),
            HeuristicCode.Ch7_2, Pattern.compile("\\breturn\\s+null\\b|\\bOptional\\b"),
            HeuristicCode.F1, Pattern.compile("\\b(public|private|protected|static)\\b.*\\(.*,"),
            HeuristicCode.G28, Pattern.compile("\\b(if|while|for)\\s*\\(|&&|\\|\\||\\?\\s*[^.]"),
            HeuristicCode.G29, Pattern.compile("!=|==|!\\s*\\(|!\\w"),
            HeuristicCode.G24, Pattern.compile(".{120,}"),
            HeuristicCode.G25, Pattern.compile("[^a-zA-Z_\"]\\d{2,}|\\b\\d+\\.\\d+\\b"),
            HeuristicCode.G5, Pattern.compile("\".*\""),
            HeuristicCode.G18, Pattern.compile("\\b(public|private|protected|static)\\b.*\\(")
    ));

    @Test
    void serveTaskFindingsAnchorAtTheirActualConstruct(@TempDir Path tempDir) throws Exception {
        runFixture("ServeTask.java", tempDir);
    }

    @Test
    void changeApplierFindingsAnchorAtTheirActualConstruct(@TempDir Path tempDir) throws Exception {
        runFixture("ChangeApplier.java", tempDir);
    }

    @Test
    void harnessRecipePassFindingsAnchorAtTheirActualConstruct(@TempDir Path tempDir) throws Exception {
        runFixture("HarnessRecipePass.java", tempDir);
    }

    private void runFixture(final String fixtureName, final Path tempDir) throws IOException, FindingSourceException {
        // Materialise the fixture into a fake project under tempDir so the
        // OpenRewrite source can pick it up via its normal source-set walk.
        final Path src = tempDir.resolve("src/main/java/com/example").resolve(fixtureName);
        Files.createDirectories(src.getParent());
        final byte[] contents = readResource("line-accuracy/" + fixtureName);
        Files.write(src, rewritePackageDeclaration(contents));

        final ProjectContext ctx = new ProjectContext(
                tempDir, "fixture", "1.0", "21",
                List.of(tempDir.resolve("src/main/java")),
                List.of(), tempDir.resolve("build"), tempDir.resolve("build/reports"), List.of());

        final List<Finding> findings = new OpenRewriteFindingSource().collectFindings(ctx);
        final List<String> sourceLines = Files.readAllLines(src);

        final List<Drift> drift = new ArrayList<>();
        for (final Finding f : findings) {
            final Pattern anchor = ANCHOR_PATTERNS.get(f.code());
            if (anchor == null) {
                continue;
            }
            if (!matchesAnyLineWithinTolerance(f.startLine(), sourceLines, anchor)) {
                drift.add(new Drift(
                        fixtureName,
                        f.code(),
                        f.startLine(),
                        lineText(sourceLines, f.startLine()),
                        f.message()));
            }
        }

        // The threshold below records the CURRENT drift level for each
        // fixture. As recipes are fixed, ratchet the numbers down — the
        // test then guards against regression. The point isn't to let
        // bad accuracy through; it's to make every line-number fix a
        // visible delta in this file.
        final long checked = findings.stream().filter(f -> ANCHOR_PATTERNS.containsKey(f.code())).count();
        final int currentDriftCap = currentDriftCapFor(fixtureName);

        final String report = "%d/%d findings in %s drifted from their heuristic anchor (current cap: %d):\n  %s".formatted(
                drift.size(), checked, fixtureName, currentDriftCap,
                drift.isEmpty() ? "(none)" : drift.stream().map(Drift::toString).collect(Collectors.joining("\n  ")));

        if (drift.size() > currentDriftCap) {
            fail(report + "\nDrift went UP — investigate whether a recent recipe change re-broke line accuracy.");
        }
        if (drift.size() < currentDriftCap) {
            fail("Drift in " + fixtureName + " is now " + drift.size() + " < cap " + currentDriftCap
                    + " — RATCHET DOWN by editing currentDriftCapFor() so the test guards the new floor.\n" + report);
        }
        // Equal: print so the run is observable in CI logs even on green.
        System.out.println("[line-accuracy] " + report);
    }

    /**
     * Today's accepted drift level. Each entry should ratchet down as
     * recipes are fixed (catch-keyword pinning, method overload
     * disambiguation, recipe lineNumber audit). When a fix lands and the
     * actual drift drops, this map gets edited too — the test fails if
     * drift went DOWN without updating the cap, which forces the cap
     * to track reality.
     */
    private static int currentDriftCapFor(final String fixtureName) {
        return switch (fixtureName) {
            case "ServeTask.java" -> 3;
            case "ChangeApplier.java" -> 3;
            case "HarnessRecipePass.java" -> 0;
            default -> 0;
        };
    }

    private static byte[] readResource(final String path) throws IOException {
        try (InputStream in = LineNumberAccuracyIntegrationTest.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("missing test resource: " + path);
            }
            return in.readAllBytes();
        }
    }

    /**
     * The fixture file has the original package declaration from the
     * plugin module. The synthetic test project lives under
     * {@code com.example}, so swap the first {@code package ...;} line
     * to match.
     */
    private static byte[] rewritePackageDeclaration(final byte[] original) {
        final String text = new String(original);
        final int newline = text.indexOf('\n');
        if (newline < 0 || !text.startsWith("package ")) {
            return original;
        }
        return ("package com.example;\n" + text.substring(newline + 1)).getBytes();
    }

    private static boolean matchesAnyLineWithinTolerance(
            final int line, final List<String> sourceLines, final Pattern anchor) {
        for (int offset = -TOLERANCE; offset <= TOLERANCE; offset++) {
            final int idx = line - 1 + offset;
            if (idx < 0 || idx >= sourceLines.size()) {
                continue;
            }
            if (anchor.matcher(sourceLines.get(idx)).find()) {
                return true;
            }
        }
        return false;
    }

    private static String lineText(final List<String> sourceLines, final int line) {
        if (line <= 0 || line > sourceLines.size()) {
            return null;
        }
        return sourceLines.get(line - 1);
    }
}
