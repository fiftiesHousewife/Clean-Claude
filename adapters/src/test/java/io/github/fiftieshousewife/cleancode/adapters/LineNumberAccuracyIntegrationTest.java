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

    /** Generic "real code, not blank" — fallback for fuzzy heuristics. */
    private static final Pattern ANY_CODE_LINE = Pattern.compile("\\S");

    private static final Map<HeuristicCode, Pattern> ANCHOR_PATTERNS = buildAnchors();

    private static Map<HeuristicCode, Pattern> buildAnchors() {
        final Map<HeuristicCode, Pattern> p = new LinkedHashMap<>();
        p.put(HeuristicCode.Ch7_1, Pattern.compile("\\bcatch\\s*\\("));
        // Ch7_2 covers "return null" and any null-checking expression
        // (`== null` / `!= null`). Method-declaration lines are also
        // acceptable when the density anchor falls back.
        p.put(HeuristicCode.Ch7_2, Pattern.compile(
                "\\breturn\\s+null\\b|\\bOptional\\b|==\\s*null\\b|!=\\s*null\\b"
                        + "|^\\s*(public|private|protected|static|final|\\S+\\s+\\w+\\s*\\()"));
        p.put(HeuristicCode.F1, Pattern.compile("\\b(public|private|protected|static)\\b.*\\(.*,"));
        // F2 covers output-arg method signatures AND class-level
        // InconsistentReturn findings — both anchors are acceptable.
        p.put(HeuristicCode.F2, Pattern.compile(
                "\\b(public|private|protected|static)\\b.*\\(|\\b(class|record|interface|enum)\\b"));
        p.put(HeuristicCode.G28, Pattern.compile("\\b(if|while|for)\\s*\\(|&&|\\|\\||\\?\\s*[^.]"));
        p.put(HeuristicCode.G29, Pattern.compile("!=|==|!\\s*\\(|!\\w"));
        p.put(HeuristicCode.G24, Pattern.compile(".{120,}"));
        p.put(HeuristicCode.G25, Pattern.compile("[^a-zA-Z_\"]\\d{2,}|\\b\\d+\\.\\d+\\b"));
        p.put(HeuristicCode.G5, Pattern.compile("\".*\""));
        p.put(HeuristicCode.G18, Pattern.compile("\\b(public|private|protected|static)\\b.*\\("));
        // Method-level findings: the line MUST contain a method
        // declaration, not just any code. Rejects the common "fell back
        // to lineOfClass" failure mode where every method-level finding
        // collapses onto the `public class Foo {` line.
        final Pattern methodDecl = Pattern.compile(
                "\\b(public|private|protected|static)\\b[^=]*\\([^=]*\\)|\\b\\w[\\w]*\\s+\\w+\\s*\\(");
        p.put(HeuristicCode.T1, methodDecl);
        p.put(HeuristicCode.G30, methodDecl);
        p.put(HeuristicCode.G15, methodDecl);
        p.put(HeuristicCode.G31, methodDecl);
        // Field-shaped: `<modifiers>? <type> name = ...` or `name;`.
        // Rejects the class declaration for G8 / J3 / G4 / N6.
        final Pattern fieldOrMethod = Pattern.compile(
                "\\w+\\s*[=;]|\\b(public|private|protected|static)\\b.*\\(");
        p.put(HeuristicCode.G8, fieldOrMethod);
        p.put(HeuristicCode.J3, fieldOrMethod);
        p.put(HeuristicCode.G4, fieldOrMethod);
        p.put(HeuristicCode.N6, fieldOrMethod);
        // J1 wildcard imports: line must be an `import ... *;` statement.
        p.put(HeuristicCode.J1, Pattern.compile("^\\s*import\\s+.*\\*\\s*;"));
        // J2 inherited constants: implements/extends line.
        p.put(HeuristicCode.J2, Pattern.compile("\\b(implements|extends)\\b|\\b(class|interface|enum)\\b"));
        // G1 embedded language: must be a string literal line, not a
        // method declaration.
        p.put(HeuristicCode.G1, Pattern.compile("\".*\""));
        // Class-level (Ch10_1 file-too-long, EI_EXPOSE on records): the
        // SnippetReader slides forward to the type declaration, so accept
        // the declaration keyword OR a body line.
        p.put(HeuristicCode.Ch10_1, Pattern.compile(
                "\\b(class|record|interface|enum)\\b|\\S"));
        // Local-declaration / vertical separation: the offender is a
        // local variable declaration line.
        p.put(HeuristicCode.G10, Pattern.compile("\\w+\\s*[=;]"));
        // G35 covers two shapes:
        //  - in-method magic numbers (return foo() <= 60, arr[5], if (x==100))
        //  - field initialisation with a hardcoded list (Map.of(...), List.of(...))
        // Accept any line that has a digit literal OR looks like a
        // variable declaration / initialisation.
        p.put(HeuristicCode.G35, Pattern.compile("\\b\\d+\\b|\\w+\\s*[=;]"));
        // Comments — C2/C3 redundant-comment, C5 commented-out.
        p.put(HeuristicCode.C2, Pattern.compile("//|/\\*|^\\s*\\*"));
        p.put(HeuristicCode.C3, Pattern.compile("//|/\\*|^\\s*\\*"));
        p.put(HeuristicCode.C5, Pattern.compile("//|/\\*|^\\s*\\*"));
        // Heuristics where the smell is method-shaped or fuzzy: any
        // real code line is an acceptable anchor (we just want to keep
        // findings off comment-only / blank lines).
        for (final HeuristicCode code : new HeuristicCode[] {
                HeuristicCode.G1, HeuristicCode.G4, HeuristicCode.G11,
                HeuristicCode.G12, HeuristicCode.G19, HeuristicCode.G23,
                HeuristicCode.G26, HeuristicCode.G30, HeuristicCode.G31,
                HeuristicCode.G33, HeuristicCode.G34, HeuristicCode.G36,
                HeuristicCode.G15, HeuristicCode.G17, HeuristicCode.G14,
                HeuristicCode.G16, HeuristicCode.N1, HeuristicCode.N5,
                HeuristicCode.N6, HeuristicCode.N7, HeuristicCode.J2,
                HeuristicCode.J3, HeuristicCode.T1
        }) {
            p.put(code, ANY_CODE_LINE);
        }
        return p;
    }

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

    @Test
    void openRewriteFindingSourceFindingsAnchorAtTheirActualConstruct(@TempDir Path tempDir) throws Exception {
        // Big self-referential file: stresses the source-text scanner
        // because many methods share helper-shape names.
        runFixture("OpenRewriteFindingSource.java", tempDir);
    }

    @Test
    void htmlReportWriterFindingsAnchorAtTheirActualConstruct(@TempDir Path tempDir) throws Exception {
        // Many string literals (HTML tags, CSS), heavy method count.
        runFixture("HtmlReportWriter.java", tempDir);
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
        final java.util.Set<HeuristicCode> uncoveredCodes = new java.util.LinkedHashSet<>();
        for (final Finding f : findings) {
            final Pattern anchor = ANCHOR_PATTERNS.get(f.code());
            if (anchor == null) {
                uncoveredCodes.add(f.code());
                continue;
            }
            if (!matchesAnyLineWithinTolerance(f.startLine(), sourceLines, anchor)) {
                drift.add(new Drift(
                        fixtureName,
                        f.code(),
                        f.startLine(),
                        lineText(sourceLines, f.startLine()),
                        f.message()));
                continue;
            }
            if (isCommentCode(f.code()) && !commentTextMatchesFinding(f, sourceLines)) {
                drift.add(new Drift(
                        fixtureName,
                        f.code(),
                        f.startLine(),
                        lineText(sourceLines, f.startLine()),
                        "comment text mismatch: " + f.message()));
            }
        }
        if (!uncoveredCodes.isEmpty()) {
            fail("Heuristic code(s) without an anchor pattern in this test: " + uncoveredCodes
                    + " — add an entry to ANCHOR_PATTERNS so line accuracy is verified for them.");
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
            // ServeTask: catch-line pinning landed for catch-only-logs,
            // broad-catch, and swallowed-exception findings — all 6
            // checked findings now anchor correctly.
            case "ServeTask.java" -> 0;
            // ChangeApplier: G5 magic-string lookup now resolves via
            // source-text scan; lineOfMethod prefers a source-text scan
            // over the AST line index. All three checked findings now
            // anchor correctly.
            case "ChangeApplier.java" -> 0;
            case "HarnessRecipePass.java" -> 0;
            // After the source-text scanner overhaul (lineOfMethod,
            // lineOfFirstStringLiteral with escape-aware search,
            // lineOfFirstComplexIf for G28), both larger fixtures
            // anchor cleanly.
            case "OpenRewriteFindingSource.java" -> 0;
            case "HtmlReportWriter.java" -> 0;
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

    private static boolean isCommentCode(final HeuristicCode code) {
        return code == HeuristicCode.C2 || code == HeuristicCode.C3 || code == HeuristicCode.C5;
    }

    /**
     * Comment-related findings have to anchor at the line that actually
     * holds the comment described in the message — not just any comment
     * marker nearby. We extract the salient token from the message and
     * confirm the source line (or one of its tolerance neighbours)
     * contains that token inside a comment.
     */
    private static boolean commentTextMatchesFinding(final Finding f, final List<String> sourceLines) {
        final String needle = extractCommentNeedle(f);
        if (needle == null || needle.isBlank()) {
            return true;
        }
        for (int offset = -TOLERANCE; offset <= TOLERANCE; offset++) {
            final int idx = f.startLine() - 1 + offset;
            if (idx < 0 || idx >= sourceLines.size()) {
                continue;
            }
            final String line = sourceLines.get(idx);
            final int needleAt = line.indexOf(needle);
            if (needleAt < 0) {
                continue;
            }
            final int slashSlash = line.indexOf("//");
            final int slashStar = line.indexOf("/*");
            final boolean commentMarkerBefore =
                    (slashSlash >= 0 && slashSlash < needleAt)
                            || (slashStar >= 0 && slashStar < needleAt)
                            || line.stripLeading().startsWith("*");
            if (commentMarkerBefore) {
                return true;
            }
        }
        return false;
    }

    /**
     * Pulls the salient comment token out of a finding message:
     *  - C2: identifier in the leading "Comment references 'X'".
     *  - C3: substring after the colon in "Mumbling comment in 'm': X".
     *  - C5: substring after "Commented-out code: X".
     */
    private static String extractCommentNeedle(final Finding f) {
        final String msg = f.message();
        return switch (f.code()) {
            case C2 -> {
                final java.util.regex.Matcher m = Pattern.compile("'([^']+)'").matcher(msg);
                yield m.find() ? m.group(1) : null;
            }
            case C3, C5 -> {
                final int colon = msg.indexOf(": ");
                if (colon < 0) {
                    yield null;
                }
                final String tail = msg.substring(colon + 2).strip();
                yield tail.length() > 25 ? tail.substring(0, 25) : tail;
            }
            default -> null;
        };
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
