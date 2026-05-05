package io.github.fiftieshousewife.cleancode.refactoring.upstreamdiff;

import io.github.fiftieshousewife.cleancode.refactoring.AddFinalRecipe;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Phase B item 4: tracks whether {@link AddFinalRecipe} can be replaced
 * by the upstream pair
 * {@code org.openrewrite.staticanalysis.FinalizeMethodArguments}
 * + {@code org.openrewrite.staticanalysis.FinalizeLocalVariables}.
 *
 * <p>Ours has a deliberately narrow scope:
 * <ul>
 *     <li>Method parameters everywhere — when not reassigned.</li>
 *     <li>Local variables inside a {@code sealed} class only.</li>
 * </ul>
 *
 * <p>Baseline finding: on the curated fixtures, upstream produces
 * byte-identical output to ours for {@code NonReassignedParameter} and
 * {@code SealedNonReassignedLocal} (the {@code equivalent} bucket).
 * Upstream additionally fires on {@code NonSealedNonReassignedLocal}
 * because {@code FinalizeLocalVariables} adds {@code final} to <em>every</em>
 * non-reassigned local regardless of class kind. That extra firing is
 * exactly the noise floor the original recipe was designed to avoid
 * (see Javadoc on {@code AddFinalRecipe}).
 *
 * <p>Replacement decision is therefore a product call, not a technical
 * one: substituting upstream broadens the noise floor by one finding
 * per non-sealed local. This baseline locks in the current divergence
 * so any future shift is visible in CI.
 */
class AddFinalDiffTest {

    @Test
    void addFinalDiffMatchesBaseline() {
        final Recipe ours = new AddFinalRecipe();
        final Recipe upstream = RecipeDiffHarness.loadUpstream(
                "org.openrewrite.staticanalysis.FinalizeMethodArguments",
                "org.openrewrite.staticanalysis.FinalizeLocalVariables");

        final RecipeDiffHarness.DiffReport report = RecipeDiffHarness.compare(
                ours, upstream, ProjectCorpus.curatedFixtures("AddFinal"));

        assertEquals(
                "total=5, bothNoOp=2, equivalent=2, divergent=0, onlyOurs=0, onlyUpstream=1",
                report.summary(),
                "summary drift; onlyOurs=" + report.onlyOurs()
                        + " onlyUpstream=" + report.onlyUpstream()
                        + " equivalent=" + report.equivalent());
    }
}
