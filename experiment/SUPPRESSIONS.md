# Proposed Suppressions for Experiment Runs

Goal: reduce noise in the next experiment run by suppressing findings we've decided are not worth fixing in this exercise, so Claude focuses on genuine code-quality improvements.

## Current baseline

**1395 total findings** across 6 modules (G5: 482, G22: 182, G30: 110, G24: 107, E1: 77, …).

## Suppression mechanisms available today

| Mechanism | Granularity | Works for |
|---|---|---|
| `cleanCode.disabledRecipes = listOf("E1", ...)` | Heuristic code, project-wide | Any code |
| `@SuppressCleanCode({G4}, reason=...)` on method / type / constructor | Single code block, source-anchored | Findings with `sourceFile` + line |
| (not yet implemented) per-file or per-package exclusion | — | — |

Annotations cannot suppress:
- `E1` dependency updates — the finding has `sourceFile=null`, no source anchor
- `G5` CPD duplication — spans two files, current matcher only checks one

## Proposed: `disabledRecipes` additions

Add to the project's `cleanCode { disabledRecipes = ... }` block in each module's build.gradle.kts (or at root). **Total suppressed: ~559 findings.**

### `E1` — outdated dependencies (77 findings)

**Every** E1 finding is a Ben-Manes "this dep has a newer version" report. These are about library versioning, not code cleanliness. They're also file-less, so they can't be annotated.

**Recommendation:** disable globally for the experiment. Leave enabled on main for the normal dependency-update workflow.

### `G5` — CPD duplication (482 findings)

465 of these are in `recipes/` — our 53 recipes share the OpenRewrite visitor pattern by design. The alternative (a shared abstract base) would create real problems: concrete recipes become harder to read, stack traces get noisy, and each recipe diverges enough that a base class ends up leaking extension points everywhere.

Samples:
- `BroadCatchRecipe` vs `CatchLogContinueRecipe` — both walk catch blocks with similar skeletons
- 11 CPD hits in `adapters/` — mostly XML-parsing boilerplate across SpotBugs / Checkstyle / PMD adapters, where the file-based XML source was just extracted into `AbstractFileBasedXmlFindingSource`

**Recommendation:** disable G5 globally for the experiment. Separately, follow up on the adapter duplication (arguably already in progress per the `AbstractFileBasedXmlFindingSource` refactor seen in the `manual-1.patch`).

## Proposed: `@SuppressCleanCode` additions

These are cases where a specific method or class has a legitimate reason to violate a heuristic. Needs human review — do not mass-annotate.

| Location | Code | Reason |
|---|---|---|
| `HtmlReportWriter`, `BuildOutputFormatter` | G30 | String-building methods naturally have multiple blank-line sections |
| `SuppressionIndex#processAnnotatedNode` | G29 | Double-negation is clearer than positive form in the parser walk |
| `CleanCodePlugin#verifySpotBugsOnClasspath` | G25 | The error message intentionally embeds the required `pluginManagement` snippet as a string constant |
| Test resource files under `src/test/resources/suppression/` | G4, G30 | Fixture files — deliberately unclean |

Expected impact: 10–20 additional findings suppressed, subject to per-case approval.

## Not proposed for suppression (examples)

Leave these enabled — the refactoring recipes can fix them mechanically:

- **G22** (final) — `AddFinalRecipe` fixes all 182
- **G24** (conventions) — Checkstyle auto-fixable style
- **J1** (star imports) — `DeleteUnusedImportRecipe`
- **G25** (magic strings) — `ExtractConstantRecipe`
- **T1** (assertion grouping) — `WrapAssertAllRecipe`
- **G10** (vertical separation) — `MoveDeclarationRecipe`

## Gaps the mechanism has

For a follow-up (not part of this phase):
1. **File-level / package-level suppression.** Right now you annotate per method/type/constructor. A `cleanCode { excludeFiles = listOf("**/generated/*") }` option would be simpler for generated code or test fixtures.
2. **CPD `otherFile`-aware suppression.** If one side of a duplication pair is suppressed, the other side should also be suppressed.
3. **`E1` file anchoring.** Attach outdated-dep findings to the relevant `build.gradle.kts` so they become annotatable.

## Next steps (after approval)

1. Update each module's build.gradle.kts with `cleanCode { disabledRecipes = listOf("E1", "G5") }`, OR promote this to a shared convention in `build-logic/`.
2. Add `@SuppressCleanCode` annotations to the locations in the table above, one commit per category for reviewability.
3. Re-run `./gradlew analyseCleanCode` and confirm the baseline dropped from 1395 to roughly 835.
4. Commit new baseline under `experiment/baseline/`.
