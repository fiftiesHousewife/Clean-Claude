# Finding-quality backlog and plan — 2026-05-03

The dogfood self-apply (1,623 findings) surfaced a cluster of issues that
make the report less actionable than it should be. This file consolidates
everything raised this session, ranks the work, and captures the tests
each fix should drive.

## Status as of this commit

Already shipped this session:

- `scripts/dogfood.sh` standardises the self-apply; CI fails on
  `docs/reports/` drift.
- JaCoCo bumped to 0.8.14 (Java 25 bytecode), `org.gradle.jvmargs=-Xss8m`
  for PMD on this codebase's deeply chained method calls.
- OpenRewrite line indexer counts `J.Block.end` whitespace
  (was missing every closing brace; method drift went from ±12+ to ±1–5).
- `FindingAggregator` deduplicates `(code, file, line, message)` —
  collapses 8 SpotBugs `EI_EXPOSE` rows on a 4-field record into 2.
- `SnippetReader` walks back from the focal line and stops at the
  first comment/blank, sliding the saved budget below — fixes
  "shows the class Javadoc instead of the body".
- `G18` (Inappropriate Static): Fix button removed, recipe removed
  from the harness sweep, message reworded to suggest *relocating*
  rather than making static (the heuristic prefers non-static).
- Magic strings re-classified from G25 → G5 (Duplication).
  `ExtractConstantRecipe` Fix moves with them.
- `G28` (Complex Conditional) now uses the recipe's captured
  `lineNumber` instead of falling back to the class line.
- `ChangeApplier` refuses Fix clicks on a dirty working tree
  (suppressions still allowed; `git restore` stays a single-command
  undo).

Tests added: `OpenRewriteFindingSourceTest.collectFindings_reportsCorrectLineForMethodAfterAnotherMethod`,
`SnippetReaderTest.slidesWindowDownWhenFocalLineDeclaresAClassPrecededByJavadoc`,
`FindingAggregatorTest` dedup case, two `ChangeApplierTest` git-state cases.

## A. Line-number accuracy (highest leverage)

The same anchoring problem hits roughly every method-level finding.
Many of the user's complaints ("highlighted wrong row", "shows the
class comment instead of the method", "catch-block findings pointing
at the method") are different surfaces of the same bug.

### A1. Pin Ch7_1 (catch-only-logs) at the catch keyword, not the method

`mapCatchLog` currently routes through `findingForMethod`, which
returns the method declaration line. The recipe already records
`exceptionType` per row; the adapter should walk the source for
`catch (...ExceptionType...) {` inside the matched method's brace
range and use that line.

**Test:** fixture with one method containing two distinct catches
(IOException, RuntimeException). Two Ch7_1 findings — each must
land on its own catch keyword line.

**Earlier attempt:** an AST-walk approach using the existing line
index returned an off-by-1 (got line 8, expected 9 for `} catch`).
Source-text scan with brace-depth tracking is the right primitive
because it sidesteps the line-index edge cases (lambdas, switch yield,
multiline annotations).

### A2. Disambiguate same-named methods (task #25)

`lineOfMethod(className, methodName)` returns the FIRST match. Real
reports have drifts of -111, +21, etc. when two methods share a name
(overloads, lambda parameter shadowing, anonymous-inner methods).

**Fix:** thread parameter signatures through `Row` so the lookup is
`(className, methodName, paramTypes)`. For finders that don't have
signatures, take the FIRST occurrence at or after the recipe's
captured row line — at minimum we'd land in the right method group.

**Test:** fixture with `foo(String)` and `foo(String, int)`, recipe
flags only the second; assert the line matches the second declaration.

### A3. Class-level findings should still snippet inside the class

After the SnippetReader Javadoc-skip fix, class-level findings show
the declaration line plus 4 lines below. That's the right behaviour
for short classes but for a 200-line class, the user wants to see
the part of the class the finding is about. Class-level findings
(Ch10_1 file-length, EI_EXPOSE on records) often have NO single
"the part" — but for those, the message itself is informative
("class is 169 lines"), and the snippet is mostly decorative.

**Decision:** leave A3 as-is. Don't try to be cleverer; the message
plus the IDE-link does the job for class-level smells.

### A4. Audit the other recipes that pass `Row.lineNumber`

Every recipe whose Row already has a `lineNumber` field should be
using it via `finding(code, className, lineNumber, message)`, not
the className-only fallback. G28 was one example just fixed. Likely
others: scan for `finding(HeuristicCode.X, r.className()` calls
where `r.lineNumber()` exists.

**Test:** for each migrated recipe, fixture-level assertion that
the finding lands on the offending construct's line, not the class.

## B. False positives & categorisation

### B1. F3 should skip private/package-private methods

User flagged `private PromptBuilder() {}` and a public delegating
overload as a false positive. F3 ("flag arguments") is about public
API design — private internal overloads where the boolean is a
non-leaky control parameter shouldn't fire.

**Fix:** in `FlagArgumentRecipe`, skip methods that are not public
(or skip those whose only callers are inside the same file).

**Test:** fixture with a public `foo(boolean)` and a private
`bar(boolean)` — only the public method emits F3.

### B2. Recipe-message vocabulary review

User feedback: "don't show messages like ... — run X recipe"; the
Fix button speaks for itself. Audit all recipe messages for
redundant suffixes pointing at recipe class names. (G12 inline
fully-qualified-references already cleaned; check the rest.)

## C. UI configuration

### C1. Configure button for G24 LineLength

User pointed out: G24 has no way to tune the 120-char limit from
the UI. The plumbing exists (`tuneThreshold` change kind in
`ChangeApplier`) — it just isn't exposed for line-length-style
configurable rules.

**Fix:** in `HtmlReportWriter`, when a finding's heuristic has a
configurable threshold (track in `RefactoringRegistry` or a new
`ConfigurableHeuristics` map), render a ⚙️ Configure button that
opens a modal asking for the new value, then stages a
`tuneThreshold` change.

**Test:** rendered HTML contains `data-action="configure"` for
G24/G25/T1 rows; clicking stages a change with kind `tuneThreshold`
and the user-entered value.

### C2. Stop button persistence

Already implemented (commit cc9a58f). Verify still works after the
serve-task changes.

## D. Outstanding work from before this session

### D1. Re-wire SystemOut → Lombok 0.7 (task #15)

Artifact is in mavenLocal. Need to:
1. Add `system-out-to-lombok-log4j = "0.7"` to `gradle/libs.versions.toml`.
2. Bump the dependency in the harness module that consumes the YAML
   recipe.
3. Either instantiate the four Java transforms via a
   `refactoring/Slf4jTransforms` wrapper, or load the YAML composite
   via `Environment.builder().scanRuntimeClasspath().build()
   .activateRecipe(...)`.
4. Verify `@Slf4j` lands only on classes that already have Lombok
   on the classpath.

## Test-drive priority order

1. **A1 catch-line pinning** — biggest user-visible win on a
   high-volume code (35 Ch7_1 findings in plugin module alone).
2. **A4 audit & migrate** — sweep through the rest of the recipes
   that have `lineNumber` but aren't using it.
3. **B1 F3 visibility filter** — quick PMD-style tweak, removes
   noise from the report.
4. **A2 method overload disambiguation** — the remaining wild
   drifts. Lower priority because the impact is narrow but the
   visible misdirection is severe when it hits.
5. **C1 Configure threshold button** — UX delight, modest.
6. **D1 SystemOut → Lombok** — feature work, separate concern.

## How we'll verify

- For every fix: a new failing test, then a passing one. The dogfood
  report numbers should also move in the right direction (fewer
  false positives → lower count; more accurate lines → no more
  user complaints when they click around).
- After each batch lands, `./scripts/dogfood.sh` and inspect deltas:
  module totals, top heuristic codes, and a sample of snippets in
  the served UI.
