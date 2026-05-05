package io.github.fiftieshousewife.cleancode.refactoring.upstreamdiff;

import io.github.fiftieshousewife.cleancode.refactoring.UseTryWithResourcesRecipe;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Phase B item 3: tracks whether this project's
 * {@link UseTryWithResourcesRecipe} can be replaced by upstream
 * {@code org.openrewrite.staticanalysis.UseTryWithResources}.
 *
 * <p>Locks in the current diff baseline against curated fixtures: ours
 * fires on {@code SimpleDeclTryFinallyClose} and {@code NullGuardedClose},
 * upstream fires on neither. Both correctly skip the five negative cases.
 *
 * <p>The most likely cause of the {@code onlyOurs} bucket is that
 * upstream gates on {@code AutoCloseable} type information, while our
 * harness parses fixtures without classpath context (mirroring the
 * production {@code HarnessRecipePass.runOne} path). Until either
 * (a) the harness is taught to parse with classpath, or (b) upstream is
 * relaxed to fire on structural matches alone, this baseline records
 * that upstream is a strict subset of ours. Replacement would lose
 * coverage in production.
 *
 * <p>This test fails if the report shape changes — which is exactly the
 * signal we want. A drop in {@code onlyOurs} means upstream caught up
 * (or ours regressed); a non-zero {@code divergent} or {@code onlyUpstream}
 * means a deeper investigation is warranted.
 */
class UseTryWithResourcesDiffTest {

    private static final int EXPECTED_BOTH_NO_OP = 5;
    private static final int EXPECTED_ONLY_OURS = 2;
    private static final int EXPECTED_DIVERGENT = 0;
    private static final int EXPECTED_ONLY_UPSTREAM = 0;
    private static final int EXPECTED_EQUIVALENT = 0;

    @Test
    void useTryWithResourcesDiffMatchesBaseline() {
        final Recipe ours = new UseTryWithResourcesRecipe();
        final Recipe upstream = RecipeDiffHarness.loadUpstream(
                "org.openrewrite.staticanalysis.UseTryWithResources");

        final RecipeDiffHarness.DiffReport report = RecipeDiffHarness.compare(
                ours, upstream, ProjectCorpus.curatedFixtures("UseTryWithResources"));

        assertEquals(EXPECTED_BOTH_NO_OP, report.bothNoOp(),
                "bothNoOp drift: " + report.summary());
        assertEquals(EXPECTED_EQUIVALENT, report.equivalent().size(),
                "equivalent drift: " + report.summary());
        assertEquals(EXPECTED_DIVERGENT, report.divergent().size(),
                "divergent drift: " + report.summary());
        assertEquals(EXPECTED_ONLY_OURS, report.onlyOurs().size(),
                "onlyOurs drift: " + report.summary() + " — onlyOurs="
                        + report.onlyOurs());
        assertEquals(EXPECTED_ONLY_UPSTREAM, report.onlyUpstream().size(),
                "onlyUpstream drift: " + report.summary() + " — onlyUpstream="
                        + report.onlyUpstream());
    }
}
