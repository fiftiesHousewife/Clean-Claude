package io.github.fiftieshousewife.cleancode.refactoring.upstreamdiff;

import io.github.fiftieshousewife.cleancode.refactoring.RestoreInterruptFlagRecipe;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Phase B item 7: tracks whether {@link RestoreInterruptFlagRecipe} can
 * be replaced by upstream
 * {@code org.openrewrite.staticanalysis.InterruptedExceptionHandling}.
 *
 * <p>Both recipes target the same anti-pattern: a {@code catch
 * (InterruptedException …)} block that fails to propagate interrupt
 * semantics. Both rely on type attribution; JDK types resolve via the
 * runtime classpath, so the harness compares both fairly.
 *
 * <p>Baseline finding: ours and upstream produce byte-identical output
 * for the canonical "catch swallows" case ({@code MissingRestore} —
 * equivalent=1). Upstream additionally fires on {@code RethrowsCaught}
 * (onlyUpstream=1), inserting {@code Thread.currentThread().interrupt();}
 * even when the catch body rethrows the caught exception. Ours
 * deliberately skips that case — the Javadoc notes that rethrowing an
 * {@code InterruptedException} preserves interrupt semantics by the
 * caller's contract, so an extra restore call is redundant.
 *
 * <p>Replacement decision: as with {@link AddFinalDiffTest}, upstream is
 * a strict superset by design. Substituting upstream broadens the fix
 * footprint by one rewrite per rethrow site.
 */
class RestoreInterruptFlagDiffTest {

    @Test
    void restoreInterruptFlagDiffMatchesBaseline() {
        final Recipe ours = new RestoreInterruptFlagRecipe();
        final Recipe upstream = RecipeDiffHarness.loadUpstream(
                "org.openrewrite.staticanalysis.InterruptedExceptionHandling");

        final RecipeDiffHarness.DiffReport report = RecipeDiffHarness.compare(
                ours, upstream, ProjectCorpus.curatedFixtures("RestoreInterruptFlag"));

        assertEquals(
                "total=4, bothNoOp=2, equivalent=1, divergent=0, onlyOurs=0, onlyUpstream=1",
                report.summary(),
                "summary drift; onlyOurs=" + report.onlyOurs()
                        + " onlyUpstream=" + report.onlyUpstream()
                        + " equivalent=" + report.equivalent());
    }
}
