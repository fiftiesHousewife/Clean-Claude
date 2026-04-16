# Proposed Suppressions for Experiment Runs

Goal: reduce noise in the next experiment run by suppressing findings we've decided are not worth fixing in this exercise, so Claude focuses on genuine code-quality improvements.

## Current baseline

**1395 total findings** across 6 modules (G5: 482, G22: 182, G30: 110, G24: 107, E1: 77, …).

## Suppression mechanisms available today

| Mechanism | Granularity | Works for |
|---|---|---|
| `cleanCode.disabledRecipes = listOf(...)` | Heuristic code, project-wide | Any code |
| `cleanCode.packageSuppressions = mapOf("pkg" to listOf("code", ...))` | Heuristic code per package prefix | Any code with a `sourceFile`; CPD also matches on `otherFile` |
| `@SuppressCleanCode({G4}, reason=...)` on method / type / constructor | Single code block, source-anchored | Findings with `sourceFile` + line |

Annotations cannot suppress:
- `E1` dependency updates — the finding has `sourceFile=null`, no source anchor

## Applied: `packageSuppressions` for `recipes/`

After discussion, E1 stays enabled (straightforward to fix by bumping dep versions). G5 and Ch7_2 are suppressed in the `org.fiftieshousewife.cleancode.recipes` package only, using the new `packageSuppressions` mechanism.

### `G5` — CPD duplication in `recipes/` (465 of 482 total)

Our 53 recipes share the OpenRewrite visitor pattern by design. The alternative (a shared abstract base) would create real problems: concrete recipes become harder to read, stack traces get noisy, and each recipe diverges enough that a base class ends up leaking extension points everywhere.

Samples:
- `BroadCatchRecipe` vs `CatchLogContinueRecipe` — both walk catch blocks with similar skeletons
- 11 CPD hits in `adapters/` (NOT suppressed) — mostly XML-parsing boilerplate; already being consolidated into `AbstractFileBasedXmlFindingSource`

### `Ch7_2` — null density in `recipes/` (28 findings)

OpenRewrite visitors return nulls by convention to indicate "no change" — our recipes inherit this. Null checks proliferate in scanners and visitors that walk the tree conditionally. This is API-imposed, not a design flaw in our code.

### Config

```kotlin
cleanCode {
    packageSuppressions = mapOf(
        "org.fiftieshousewife.cleancode.recipes" to listOf("G5", "Ch7_2")
    )
}
```

Matcher notes: the prefix matches against the sourceFile path (converted from `org.pkg` to `org/pkg`). For CPD (G5), `otherFile` metadata is also checked so cross-file duplication with at least one leg in the package is suppressed.

### E1 — kept enabled

77 findings, one per outdated dependency. We'll fix these by bumping versions, not suppressing.

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
